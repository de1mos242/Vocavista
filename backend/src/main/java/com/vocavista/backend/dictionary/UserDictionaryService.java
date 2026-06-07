package com.vocavista.backend.dictionary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocavista.backend.api.model.DictionaryReviewItem;
import com.vocavista.backend.api.model.DictionaryReviewResponse;
import com.vocavista.backend.api.model.DictionaryReviewSubmitRequest;
import com.vocavista.backend.api.model.DictionaryReviewSubmitResponse;
import com.vocavista.backend.api.model.DictionaryVideoManifestItem;
import com.vocavista.backend.api.model.DictionaryVideoManifestResponse;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.UserAccount;
import com.vocavista.backend.media.pronunciation.PronunciationAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAssetStatus;
import com.vocavista.backend.media.pronunciation.PronunciationRepository;
import com.vocavista.backend.wordinfo.WordInfoRecord;
import java.net.URI;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDictionaryService {

	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT = 10;
	private static final int MAX_INTERVAL_DAYS = 30;
	private static final LocalTime REVIEW_DAY_START = LocalTime.of(4, 0);

	private final UserDictionaryEntryRepository entryRepository;
	private final CurrentUserService currentUserService;
	private final PronunciationRepository pronunciationRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock = Clock.systemUTC();

	@Transactional
	public void ensureEntryForCurrentUser(WordInfoRecord wordInfoRecord) {
		UserAccount userAccount = currentUserService.getCurrentUserAccount();
		ensureEntry(userAccount, wordInfoRecord);
	}

	@Transactional(readOnly = true)
	DictionaryReviewResponse getReviewItems(Integer limit, Boolean includeUpcoming) {
		UserAccount userAccount = currentUserService.getCurrentUserAccount();
		int sanitizedLimit = sanitizeLimit(limit);
		List<UserDictionaryEntry> entries = Boolean.TRUE.equals(includeUpcoming)
				? entryRepository.findByUserAccountIdOrderByDueAtAsc(userAccount.getId(), PageRequest.of(0, sanitizedLimit))
				: entryRepository.findByUserAccountIdAndDueAtLessThanEqualOrderByDueAtAsc(userAccount.getId(), now(),
						PageRequest.of(0, sanitizedLimit));
		return new DictionaryReviewResponse(entries.stream().map(this::toReviewItem).toList());
	}

	@Transactional(readOnly = true)
	DictionaryVideoManifestResponse getVideoManifest() {
		UserAccount userAccount = currentUserService.getCurrentUserAccount();
		List<DictionaryVideoManifestItem> items = entryRepository
				.findByUserAccountIdOrderByNormalizedWordAsc(userAccount.getId())
				.stream()
				.map(UserDictionaryEntry::getWordInfoRecord)
				.map(this::latestCompletedPronunciation)
				.flatMap(Optional::stream)
				.map(this::toVideoManifestItem)
				.toList();
		return new DictionaryVideoManifestResponse(items);
	}

	@Transactional
	DictionaryReviewSubmitResponse submitReview(UUID entryId, DictionaryReviewSubmitRequest request) {
		if (request == null || request.getCorrect() == null) {
			throw new DictionaryValidationException("correct is required");
		}

		UserAccount userAccount = currentUserService.getCurrentUserAccount();
		UserDictionaryEntry entry = entryRepository.findByIdAndUserAccountId(entryId, userAccount.getId())
				.orElseThrow(() -> new DictionaryNotFoundException("Dictionary entry was not found"));
		WordInfoResponse wordInfo = readWordInfo(entry.getWordInfoRecord());
		String expectedAnswer = expectedAnswer(wordInfo);
		OffsetDateTime now = now();

		entry.setRepetitionCount(entry.getRepetitionCount() + 1);
		entry.setLastReviewedAt(now);
		if (Boolean.TRUE.equals(request.getCorrect())) {
			entry.setLastResult(ReviewResult.CORRECT);
			entry.setCorrectStreak(entry.getCorrectStreak() + 1);
			entry.setEaseFactor(Math.min(3.0, entry.getEaseFactor() + 0.1));
			entry.setIntervalDays(nextCorrectInterval(entry));
		}
		else {
			entry.setLastResult(ReviewResult.INCORRECT);
			entry.setCorrectStreak(0);
			entry.setLapseCount(entry.getLapseCount() + 1);
			entry.setEaseFactor(Math.max(1.3, entry.getEaseFactor() - 0.2));
			entry.setIntervalDays(1);
		}
		entry.setDueAt(nextReviewDayStart(now, entry.getIntervalDays()));
		entry.setUpdatedAt(now);

		return new DictionaryReviewSubmitResponse(entry.getId(), request.getCorrect(), expectedAnswer, entry.getDueAt(),
				entry.getIntervalDays(), entry.getCorrectStreak(), entry.getRepetitionCount(), entry.getLapseCount());
	}

	private UserDictionaryEntry ensureEntry(UserAccount userAccount, WordInfoRecord wordInfoRecord) {
		OffsetDateTime now = now();
		return entryRepository.findByUserAccountIdAndNormalizedWord(userAccount.getId(), wordInfoRecord.getNormalizedWord())
				.map(entry -> {
					entry.refreshWordInfoRecord(wordInfoRecord, now);
					return entry;
				})
				.orElseGet(() -> createEntry(userAccount, wordInfoRecord, now));
	}

	private UserDictionaryEntry createEntry(UserAccount userAccount, WordInfoRecord wordInfoRecord, OffsetDateTime now) {
		try {
			return entryRepository.save(UserDictionaryEntry.create(userAccount, wordInfoRecord, now));
		}
		catch (DataIntegrityViolationException ex) {
			return entryRepository.findByUserAccountIdAndNormalizedWord(userAccount.getId(), wordInfoRecord.getNormalizedWord())
					.map(entry -> {
						entry.refreshWordInfoRecord(wordInfoRecord, now);
						return entry;
					})
					.orElseThrow(() -> ex);
		}
	}

	private DictionaryReviewItem toReviewItem(UserDictionaryEntry entry) {
		WordInfoResponse wordInfo = readWordInfo(entry.getWordInfoRecord());
		DictionaryReviewItem item = new DictionaryReviewItem(entry.getId(), entry.getWordInfoRecord().getId(),
				entry.getNormalizedWord(), expectedAnswer(wordInfo), wordInfo.getTranslations(), wordInfo.getPartOfSpeech(),
				entry.getDueAt());
		item.setArticle(wordInfo.getArticle());
		pronunciationRepository
				.findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc(entry.getWordInfoRecord().getId(),
						PronunciationAssetStatus.COMPLETED)
				.ifPresent(asset -> item.setPronunciationAssetId(asset.getId()));
		return item;
	}

	private Optional<PronunciationAsset> latestCompletedPronunciation(WordInfoRecord wordInfoRecord) {
		return pronunciationRepository.findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc(
				wordInfoRecord.getId(), PronunciationAssetStatus.COMPLETED);
	}

	private DictionaryVideoManifestItem toVideoManifestItem(PronunciationAsset asset) {
		return new DictionaryVideoManifestItem(asset.getId(), asset.getWordInfoRecord().getId(), smallVideoUri(asset),
				fullVideoUri(asset), asset.getUpdatedAt());
	}

	private static URI smallVideoUri(PronunciationAsset asset) {
		return URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video/small");
	}

	private static URI fullVideoUri(PronunciationAsset asset) {
		return URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video");
	}

	private WordInfoResponse readWordInfo(WordInfoRecord record) {
		try {
			WordInfoResponse response = objectMapper.readValue(record.getResponseJson(), WordInfoResponse.class);
			response.setId(record.getId());
			return response;
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Could not read stored word info response", ex);
		}
	}

	private static String expectedAnswer(WordInfoResponse wordInfo) {
		if (wordInfo.getPartOfSpeech() == PartOfSpeech.NOUN && wordInfo.getArticle() != null) {
			return wordInfo.getArticle().getValue() + " " + wordInfo.getNormalizedWord();
		}
		return wordInfo.getNormalizedWord();
	}

	private static int nextCorrectInterval(UserDictionaryEntry entry) {
		if (entry.getCorrectStreak() == 1) {
			return 1;
		}
		if (entry.getCorrectStreak() == 2) {
			return 3;
		}
		int previousInterval = Math.max(1, entry.getIntervalDays());
		return Math.min(MAX_INTERVAL_DAYS, Math.max(1, (int) Math.round(previousInterval * entry.getEaseFactor())));
	}

	private static int sanitizeLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		if (limit < 1) {
			throw new DictionaryValidationException("limit must be at least 1");
		}
		return Math.min(MAX_LIMIT, limit);
	}

	private OffsetDateTime nextReviewDayStart(OffsetDateTime reviewedAt, int intervalDays) {
		OffsetDateTime utc = reviewedAt.withOffsetSameInstant(ZoneOffset.UTC);
		OffsetDateTime reviewDayStart = utc.toLocalTime().isBefore(REVIEW_DAY_START)
				? utc.toLocalDate().minusDays(1).atTime(REVIEW_DAY_START).atOffset(ZoneOffset.UTC)
				: utc.toLocalDate().atTime(REVIEW_DAY_START).atOffset(ZoneOffset.UTC);
		return reviewDayStart.plusDays(intervalDays);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(clock);
	}

}
