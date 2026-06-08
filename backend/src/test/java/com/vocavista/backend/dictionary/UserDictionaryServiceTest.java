package com.vocavista.backend.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.DictionaryReviewSubmitRequest;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.UserAccount;
import com.vocavista.backend.media.pronunciation.PronunciationAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAssetStatus;
import com.vocavista.backend.media.pronunciation.PronunciationRepository;
import com.vocavista.backend.media.pronunciation.PhraseImageRepository;
import com.vocavista.backend.wordinfo.WordInfoRecord;
import com.vocavista.backend.wordinfo.WordInfoRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserDictionaryServiceTest {

	@Mock
	private UserDictionaryEntryRepository entryRepository;

	@Mock
	private CurrentUserService currentUserService;

	@Mock
	private PronunciationRepository pronunciationRepository;

	@Mock
	private PhraseImageRepository phraseImageRepository;

	@Mock
	private WordInfoRepository wordInfoRepository;

	@Test
	void createsEntryForCurrentUserWhenMissing() {
		UserAccount userAccount = userAccount();
		WordInfoRecord wordInfoRecord = wordInfoRecord("Hausaufgabe");
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdAndNormalizedWord(userAccount.getId(), "Hausaufgabe"))
				.thenReturn(Optional.empty());
		when(entryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service().ensureEntryForCurrentUser(wordInfoRecord);

		verify(entryRepository).save(any(UserDictionaryEntry.class));
	}

	@Test
	void updatesExistingEntryWithoutResettingReviewState() {
		UserAccount userAccount = userAccount();
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, wordInfoRecord("Hausaufgabe"), OffsetDateTime.now());
		entry.setCorrectStreak(2);
		WordInfoRecord refreshedRecord = wordInfoRecord("Hausaufgabe");
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdAndNormalizedWord(userAccount.getId(), "Hausaufgabe"))
				.thenReturn(Optional.of(entry));

		service().ensureEntryForCurrentUser(refreshedRecord);

		assertThat(entry.getWordInfoRecord()).isEqualTo(refreshedRecord);
		assertThat(entry.getCorrectStreak()).isEqualTo(2);
		verify(entryRepository, never()).save(any());
	}

	@Test
	void dueReviewReturnsOnlyDueEntriesByDefault() {
		UserAccount userAccount = userAccount();
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);

		service().getReviewItems(10, false);

		verify(entryRepository).findByUserAccountIdAndDueAtLessThanEqualOrderByDueAtAsc(
				any(UUID.class), any(OffsetDateTime.class), any(Pageable.class));
		verify(entryRepository, never()).findByUserAccountIdOrderByDueAtAsc(any(), any());
	}

	@Test
	void includeUpcomingReturnsNextEntriesByDueAt() {
		UserAccount userAccount = userAccount();
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);

		service().getReviewItems(10, true);

		verify(entryRepository).findByUserAccountIdOrderByDueAtAsc(any(UUID.class), any(Pageable.class));
		verify(entryRepository, never()).findByUserAccountIdAndDueAtLessThanEqualOrderByDueAtAsc(any(), any(), any());
	}

	@Test
	void reviewItemsIncludeCompletedPronunciationAssetId() {
		UserAccount userAccount = userAccount();
		WordInfoRecord wordInfoRecord = wordInfoRecord("Hausaufgabe");
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, wordInfoRecord, OffsetDateTime.now());
		PronunciationAsset asset = pronunciationAsset(wordInfoRecord);
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdAndDueAtLessThanEqualOrderByDueAtAsc(
				any(UUID.class), any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(entry));
		when(pronunciationRepository.findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc(
				wordInfoRecord.getId(), PronunciationAssetStatus.COMPLETED)).thenReturn(Optional.of(asset));
		when(phraseImageRepository.findFirstByWordInfoRecordIdAndNormalizedPhraseAndStatusOrderByUpdatedAtDesc(
				any(UUID.class), any(), any())).thenReturn(Optional.empty());

		var response = service().getReviewItems(10, false);

		assertThat(response.getItems().getFirst().getPronunciationAssetId()).isEqualTo(asset.getId());
		assertThat(response.getItems().getFirst().getPhrase()).isEqualTo(asset.getNormalizedPhrase());
	}

	@Test
	void videoManifestIncludesLatestCompletedDictionaryPronunciation() {
		UserAccount userAccount = userAccount();
		WordInfoRecord wordInfoRecord = wordInfoRecord("Hausaufgabe");
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, wordInfoRecord, OffsetDateTime.now());
		PronunciationAsset asset = pronunciationAsset(wordInfoRecord);
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdOrderByNormalizedWordAsc(userAccount.getId())).thenReturn(List.of(entry));
		when(pronunciationRepository.findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc(
				wordInfoRecord.getId(), PronunciationAssetStatus.COMPLETED)).thenReturn(Optional.of(asset));

		var response = service().getVideoManifest();

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().getFirst().getPronunciationAssetId()).isEqualTo(asset.getId());
		assertThat(response.getItems().getFirst().getVideoUrl())
				.hasToString("/api/v1/media/pronunciations/" + asset.getId() + "/video/small");
		assertThat(response.getItems().getFirst().getFullVideoUrl())
				.hasToString("/api/v1/media/pronunciations/" + asset.getId() + "/video");
	}

	@Test
	void correctReviewUpdatesSrsStateAndReturnsExpectedAnswer() {
		UserAccount userAccount = userAccount();
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, wordInfoRecord("Hausaufgabe"), OffsetDateTime.now());
		DictionaryReviewSubmitRequest request = new DictionaryReviewSubmitRequest(true);
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByIdAndUserAccountId(entry.getId(), userAccount.getId())).thenReturn(Optional.of(entry));

		var response = service().submitReview(entry.getId(), request);

		assertThat(response.getCorrect()).isTrue();
		assertThat(response.getExpectedAnswer()).isEqualTo("die Hausaufgabe");
		assertThat(entry.getLastResult()).isEqualTo(ReviewResult.CORRECT);
		assertThat(entry.getIntervalDays()).isEqualTo(1);
		assertThat(entry.getCorrectStreak()).isEqualTo(1);
		assertThat(entry.getDueAt().getOffset()).isEqualTo(ZoneOffset.UTC);
		assertThat(entry.getDueAt().toLocalTime()).isEqualTo(java.time.LocalTime.of(4, 0));
	}

	@Test
	void incorrectReviewResetsStreakAndIncrementsLapses() {
		UserAccount userAccount = userAccount();
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, wordInfoRecord("Hausaufgabe"), OffsetDateTime.now());
		entry.setCorrectStreak(3);
		entry.setIntervalDays(10);
		DictionaryReviewSubmitRequest request = new DictionaryReviewSubmitRequest(false);
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByIdAndUserAccountId(entry.getId(), userAccount.getId())).thenReturn(Optional.of(entry));

		service().submitReview(entry.getId(), request);

		assertThat(entry.getLastResult()).isEqualTo(ReviewResult.INCORRECT);
		assertThat(entry.getCorrectStreak()).isZero();
		assertThat(entry.getLapseCount()).isEqualTo(1);
		assertThat(entry.getIntervalDays()).isEqualTo(1);
	}

	private UserDictionaryService service() {
		return new UserDictionaryService(entryRepository, currentUserService, pronunciationRepository, phraseImageRepository,
				wordInfoRepository);
	}

	private static PronunciationAsset pronunciationAsset(WordInfoRecord wordInfoRecord) {
		PronunciationAsset asset = new PronunciationAsset();
		asset.setId(UUID.randomUUID());
		asset.setWordInfoRecord(wordInfoRecord);
		asset.setNormalizedWord(wordInfoRecord.getNormalizedWord());
		asset.setNormalizedPhrase("Ich mache meine Hausaufgabe.");
		asset.setInputWord(wordInfoRecord.getNormalizedWord());
		asset.setInputPhrase("Ich mache meine Hausaufgabe.");
		asset.setLanguage("de");
		asset.setStatus(PronunciationAssetStatus.COMPLETED);
		asset.setVideoObjectKey("pronunciations/%s/video.mp4".formatted(asset.getId()));
		asset.setContentHash("hash");
		asset.setCreatedAt(OffsetDateTime.now());
		asset.setUpdatedAt(OffsetDateTime.now());
		asset.setCompletedAt(OffsetDateTime.now());
		return asset;
	}

	private static UserAccount userAccount() {
		UserAccount account = new UserAccount();
		account.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
		account.setProviderSubject("google-sub");
		account.setEmail("learner@example.com");
		account.setDisplayName("German Learner");
		account.setCreatedAt(OffsetDateTime.now());
		account.setUpdatedAt(OffsetDateTime.now());
		account.setLastLoginAt(OffsetDateTime.now());
		return account;
	}

	private static WordInfoRecord wordInfoRecord(String normalizedWord) {
		WordInfoRecord record = new WordInfoRecord();
		record.setId(UUID.randomUUID());
		record.setNormalizedQuery(normalizedWord.toLowerCase());
		record.setNormalizedWord(normalizedWord);
		record.setLanguage("de");
		record.setResponseJson("""
				{
				  "id": "%s",
				  "normalizedWord": "%s",
				  "language": "de",
				  "translations": {
				    "en": ["homework"],
				    "ru": ["домашнее задание"]
				  },
				  "partOfSpeech": "noun",
				  "gender": "feminine",
				  "article": "die",
				  "plural": "Hausaufgaben",
				  "frequency": "common",
				  "isCompound": true,
				  "compoundParts": [],
				  "shortNote": {
				    "en": ["A common word for school homework."],
				    "ru": ["Обычное слово для домашнего задания."]
				  },
				  "examples": []
				}
				""".formatted(record.getId(), normalizedWord));
		record.setCreatedAt(OffsetDateTime.now());
		record.setUpdatedAt(OffsetDateTime.now());
		return record;
	}

}
