package com.vocavista.backend.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.DictionaryReviewSubmitRequest;
import com.vocavista.backend.auth.CurrentUserService;
import com.vocavista.backend.auth.UserAccount;
import com.vocavista.backend.media.MediaAssetQueryService;
import com.vocavista.backend.media.pronunciation.PronunciationAsset;
import com.vocavista.backend.media.pronunciation.PronunciationAssetStatus;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemTranslation;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import java.net.URI;
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
	private MediaAssetQueryService mediaAssetQueryService;

	@Mock
	private VocabularyItemRepository vocabularyItemRepository;

	@Test
	void createsEntryForCurrentUserWhenMissing() {
		UserAccount userAccount = userAccount();
		VocabularyItem vocabularyItem = vocabularyItem("Hausaufgabe");
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdAndVocabularyItemId(userAccount.getId(), vocabularyItem.getId()))
				.thenReturn(Optional.empty());
		when(entryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service().ensureEntryForCurrentUser(vocabularyItem);

		verify(entryRepository).save(any(UserDictionaryEntry.class));
	}

	@Test
	void updatesExistingEntryWithoutResettingReviewState() {
		UserAccount userAccount = userAccount();
		VocabularyItem vocabularyItem = vocabularyItem("Hausaufgabe");
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, vocabularyItem, OffsetDateTime.now());
		entry.setCorrectStreak(2);
		VocabularyItem refreshedItem = vocabularyItem("Hausaufgabe");
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdAndVocabularyItemId(userAccount.getId(), refreshedItem.getId()))
				.thenReturn(Optional.of(entry));

		service().ensureEntryForCurrentUser(refreshedItem);

		assertThat(entry.getVocabularyItem()).isEqualTo(refreshedItem);
		assertThat(entry.getCorrectStreak()).isEqualTo(2);
		verify(entryRepository, never()).save(any());
	}

	@Test
	void reviewItemsIncludeCompletedPronunciationAssetId() {
		UserAccount userAccount = userAccount();
		VocabularyItem vocabularyItem = vocabularyItem("Hausaufgabe");
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, vocabularyItem, OffsetDateTime.now());
		PronunciationAsset asset = pronunciationAsset(vocabularyItem);
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdAndDueAtLessThanEqualOrderByDueAtAsc(
				any(UUID.class), any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(entry));
		when(mediaAssetQueryService.latestCompletedPronunciation(vocabularyItem.getId()))
				.thenReturn(Optional.of(completedPronunciation(asset)));
		when(mediaAssetQueryService.latestCompletedPhraseImage(vocabularyItem.getId(), asset.getInputPhrase()))
				.thenReturn(Optional.empty());

		var response = service().getReviewItems(10, false);

		assertThat(response.getItems().getFirst().getPronunciationAssetId()).isEqualTo(asset.getId());
		assertThat(response.getItems().getFirst().getPhrase()).isEqualTo(asset.getInputPhrase());
	}

	@Test
	void videoManifestIncludesLatestCompletedDictionaryPronunciation() {
		UserAccount userAccount = userAccount();
		VocabularyItem vocabularyItem = vocabularyItem("Hausaufgabe");
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, vocabularyItem, OffsetDateTime.now());
		PronunciationAsset asset = pronunciationAsset(vocabularyItem);
		when(currentUserService.getCurrentUserAccount()).thenReturn(userAccount);
		when(entryRepository.findByUserAccountIdOrderByVocabularyItemWordAsc(userAccount.getId())).thenReturn(List.of(entry));
		when(mediaAssetQueryService.latestCompletedPronunciation(vocabularyItem.getId()))
				.thenReturn(Optional.of(completedPronunciation(asset)));

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
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, vocabularyItem("Hausaufgabe"), OffsetDateTime.now());
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
		UserDictionaryEntry entry = UserDictionaryEntry.create(userAccount, vocabularyItem("Hausaufgabe"), OffsetDateTime.now());
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
		return new UserDictionaryService(entryRepository, currentUserService, mediaAssetQueryService, vocabularyItemRepository,
				new DictionaryMapper());
	}

	private static MediaAssetQueryService.CompletedPronunciation completedPronunciation(PronunciationAsset asset) {
		return new MediaAssetQueryService.CompletedPronunciation(asset.getId(), asset.getVocabularyItem().getId(),
				asset.getInputPhrase(), URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video/small"),
				URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video"), asset.getUpdatedAt());
	}

	private static PronunciationAsset pronunciationAsset(VocabularyItem vocabularyItem) {
		PronunciationAsset asset = new PronunciationAsset();
		asset.setId(UUID.randomUUID());
		asset.setVocabularyItem(vocabularyItem);
		asset.setInputWord(vocabularyItem.getWord());
		asset.setInputPhrase("Ich mache meine Hausaufgabe.");
		asset.setLanguage("de");
		asset.setStatus(PronunciationAssetStatus.COMPLETED);
		asset.setVideoObjectKey("pronunciations/%s/video.mp4".formatted(asset.getId()));
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

	private static VocabularyItem vocabularyItem(String word) {
		VocabularyItem item = new VocabularyItem();
		OffsetDateTime now = OffsetDateTime.now();
		item.setId(UUID.randomUUID());
		item.setLanguage("de");
		item.setWord(word);
		item.setPhrase("Ich mache meine Hausaufgabe.");
		item.setPartOfSpeech("noun");
		item.setGender("feminine");
		item.setPlural("Hausaufgaben");
		item.setFrequency("common");
		item.setCompound(true);
		item.setCreatedAt(now);
		item.setUpdatedAt(now);
		item.getTranslations().add(translation(item, "en", "homework", "I do my homework."));
		item.getTranslations().add(translation(item, "ru", "домашнее задание", "Я делаю домашнее задание."));
		return item;
	}

	private static VocabularyItemTranslation translation(VocabularyItem item, String language, String word, String phrase) {
		VocabularyItemTranslation translation = new VocabularyItemTranslation();
		OffsetDateTime now = OffsetDateTime.now();
		translation.setId(UUID.randomUUID());
		translation.setVocabularyItem(item);
		translation.setLanguage(language);
		translation.setWordTranslation(word);
		translation.setPhraseTranslation(phrase);
		translation.setCreatedAt(now);
		translation.setUpdatedAt(now);
		return translation;
	}

}
