package com.vocavista.backend.dictionary;

import com.vocavista.backend.api.model.AddDictionaryEntryRequest;
import com.vocavista.backend.api.model.AddDictionaryEntryResponse;
import com.vocavista.backend.api.model.DictionaryReviewItem;
import com.vocavista.backend.api.model.DictionaryReviewResponse;
import com.vocavista.backend.api.model.DictionaryReviewSubmitRequest;
import com.vocavista.backend.api.model.DictionaryReviewSubmitResponse;
import com.vocavista.backend.api.model.DictionaryVideoManifestResponse;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.UserAccount;
import com.vocavista.backend.media.MediaAssetQueryService;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
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
	private final MediaAssetQueryService mediaAssetQueryService;
	private final VocabularyItemRepository vocabularyItemRepository;
	private final DictionaryMapper dictionaryMapper;
	private final Clock clock = Clock.systemUTC();

	@Transactional
	public void ensureEntryForCurrentUser(VocabularyItem vocabularyItem) {
		UserAccount userAccount = currentUserService.getCurrentUserAccount();
		ensureEntry(userAccount, vocabularyItem);
	}

	@Transactional
	AddDictionaryEntryResponse addEntry(AddDictionaryEntryRequest request) {
		if (request == null || request.getWordInfoId() == null) {
			throw new DictionaryValidationException("wordInfoId is required");
		}

		UserAccount userAccount = currentUserService.getCurrentUserAccount();
		VocabularyItem vocabularyItem = vocabularyItemRepository.findById(request.getWordInfoId())
				.orElseThrow(() -> new DictionaryNotFoundException("Vocabulary item was not found"));
		UserDictionaryEntry entry = ensureEntry(userAccount, vocabularyItem);
		return new AddDictionaryEntryResponse(entry.getId(), entry.getVocabularyItem().getId(), entry.getVocabularyItem().getWord(),
				entry.getDueAt());
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
		List<com.vocavista.backend.api.model.DictionaryVideoManifestItem> items = entryRepository
				.findByUserAccountIdOrderByVocabularyItemWordAsc(userAccount.getId())
				.stream()
				.map(UserDictionaryEntry::getVocabularyItem)
				.map(vocabularyItem -> mediaAssetQueryService.latestCompletedPronunciation(vocabularyItem.getId()))
				.flatMap(Optional::stream)
				.map(dictionaryMapper::toVideoManifestItem)
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
		String expectedAnswer = expectedAnswer(entry.getVocabularyItem());
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

	private UserDictionaryEntry ensureEntry(UserAccount userAccount, VocabularyItem vocabularyItem) {
		OffsetDateTime now = now();
		return entryRepository.findByUserAccountIdAndVocabularyItemId(userAccount.getId(), vocabularyItem.getId())
				.map(entry -> {
					entry.refreshVocabularyItem(vocabularyItem, now);
					return entry;
				})
				.orElseGet(() -> createEntry(userAccount, vocabularyItem, now));
	}

	private UserDictionaryEntry createEntry(UserAccount userAccount, VocabularyItem vocabularyItem, OffsetDateTime now) {
		try {
			return entryRepository.save(UserDictionaryEntry.create(userAccount, vocabularyItem, now));
		}
		catch (DataIntegrityViolationException ex) {
			return entryRepository.findByUserAccountIdAndVocabularyItemId(userAccount.getId(), vocabularyItem.getId())
					.map(entry -> {
						entry.refreshVocabularyItem(vocabularyItem, now);
						return entry;
					})
					.orElseThrow(() -> ex);
		}
	}

	private DictionaryReviewItem toReviewItem(UserDictionaryEntry entry) {
		DictionaryReviewItem item = dictionaryMapper.toReviewItem(entry, expectedAnswer(entry.getVocabularyItem()));
		mediaAssetQueryService.latestCompletedPronunciation(entry.getVocabularyItem().getId()).ifPresent(pronunciation -> {
			item.setPronunciationAssetId(pronunciation.id());
			item.setPhrase(pronunciation.phrase());
			mediaAssetQueryService.latestCompletedPhraseImage(entry.getVocabularyItem().getId(), pronunciation.phrase()).ifPresent(image -> {
				item.setPhraseImageId(image.id());
				item.setPhraseImageUrl(image.imageUrl());
			});
		});
		return item;
	}

	private static String expectedAnswer(VocabularyItem item) {
		String article = articleFor(item);
		return article == null ? item.getWord() : article + " " + item.getWord();
	}

	private static String articleFor(VocabularyItem item) {
		if (!"noun".equals(item.getPartOfSpeech()) || item.getGender() == null) {
			return null;
		}
		return switch (item.getGender()) {
			case "masculine" -> "der";
			case "feminine" -> "die";
			case "neuter" -> "das";
			default -> null;
		};
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
