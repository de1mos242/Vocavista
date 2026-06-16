package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.PronunciationRequest;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.dictionary.UserDictionaryService;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class PronunciationServiceTest {

	@Mock
	private PronunciationRepository pronunciationRepository;

	@Mock
	private PronunciationGenerationProcessor generationProcessor;

	@Mock
	private PronunciationVideoCompressor pronunciationVideoCompressor;

	@Mock
	private MediaStorageService mediaStorageService;

	@Mock
	private VocabularyItemRepository vocabularyItemRepository;

	@Mock
	private UserDictionaryService userDictionaryService;

	@Test
	void createsQueuedAssetAndStartsGeneration() {
		when(pronunciationRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(),
				"Ich mache meine Hausaufgabe nach dem Abendessen."))
				.thenReturn(Optional.empty());
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));
		when(pronunciationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		PronunciationService service = service();

		PronunciationResponse response = service.create(request(" Hausaufgabe ",
				" Ich mache meine Hausaufgabe nach dem Abendessen. "));

		assertThat(response.getId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(PronunciationStatus.QUEUED);
		verify(userDictionaryService).ensureEntryForCurrentUser(any(VocabularyItem.class));
		verify(generationProcessor).process(response.getId());
	}

	@Test
	void reusesExistingCachedVideoAsset() {
		PronunciationAsset existingAsset = completedVideoAsset();
		when(pronunciationRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(),
				"Ich mache meine Hausaufgabe nach dem Abendessen."))
				.thenReturn(Optional.of(existingAsset));
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));
		PronunciationService service = service();

		PronunciationResponse response = service.create(request("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen."));

		assertThat(response.getId()).isEqualTo(existingAsset.getId());
		assertThat(response.getStatus()).isEqualTo(PronunciationStatus.COMPLETED);
		assertThat(response.getVideoUrl())
				.hasToString("/api/v1/media/pronunciations/" + existingAsset.getId() + "/video/small");
		assertThat(response.getFullVideoUrl())
				.hasToString("/api/v1/media/pronunciations/" + existingAsset.getId() + "/video");
		verify(userDictionaryService).ensureEntryForCurrentUser(any(VocabularyItem.class));
		verify(pronunciationRepository, never()).save(any());
		verify(generationProcessor, never()).process(any());
	}

	@Test
	void retriesExistingFailedAsset() {
		PronunciationAsset existingAsset = failedAsset();
		when(pronunciationRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(),
				"Ich mache meine Hausaufgabe nach dem Abendessen."))
				.thenReturn(Optional.of(existingAsset));
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));
		when(pronunciationRepository.save(existingAsset)).thenReturn(existingAsset);
		PronunciationService service = service();

		PronunciationResponse response = service.create(request("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen."));

		assertThat(response.getId()).isEqualTo(existingAsset.getId());
		assertThat(response.getStatus()).isEqualTo(PronunciationStatus.QUEUED);
		assertThat(response.getErrorCode()).isNull();
		assertThat(response.getErrorMessage()).isNull();
		assertThat(existingAsset.getErrorCode()).isNull();
		assertThat(existingAsset.getErrorMessage()).isNull();
		assertThat(existingAsset.getVideoObjectKey()).isNotNull();
		verify(pronunciationRepository).save(existingAsset);
		verify(generationProcessor).process(existingAsset.getId());
	}

	@Test
	void startsGenerationAfterCommitWhenTransactionSynchronizationIsActive() {
		when(pronunciationRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(),
				"Ich mache meine Hausaufgabe nach dem Abendessen."))
				.thenReturn(Optional.empty());
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));
		when(pronunciationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		PronunciationService service = service();
		TransactionSynchronizationManager.initSynchronization();
		try {
			PronunciationResponse response = service.create(request(" Hausaufgabe ",
					" Ich mache meine Hausaufgabe nach dem Abendessen. "));

			verify(generationProcessor, never()).process(any());
			TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());

			verify(generationProcessor).process(response.getId());
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void rejectsBlankPhraseAfterTrimming() {
		PronunciationService service = service();

		assertThatThrownBy(() -> service.create(request("Hausaufgabe", "   ")))
				.isInstanceOf(PronunciationValidationException.class);
	}

	@Test
	void createsSmallVideoForExistingAssetOnFirstSmallVideoRequest() {
		PronunciationAsset asset = completedVideoAsset();
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(mediaStorageService.read(asset.getVideoObjectKey())).thenReturn(new StoredMedia("video/mp4", "video".getBytes()));
		when(pronunciationVideoCompressor.compress(any()))
				.thenReturn(Optional.of(new GeneratedVideo("small".getBytes(), "video/mp4")));
		PronunciationService service = service();

		StoredMedia smallVideo = service.getSmallVideo(asset.getId());

		assertThat(smallVideo.bytes()).isEqualTo("small".getBytes());
		assertThat(asset.getSmallVideoObjectKey()).endsWith("/video-small.mp4");
		verify(mediaStorageService).store(asset.getSmallVideoObjectKey(), "video/mp4", "small".getBytes());
		verify(pronunciationRepository).save(asset);
	}

	private PronunciationService service() {
		return new PronunciationService(pronunciationRepository, generationProcessor, pronunciationVideoCompressor,
				mediaStorageService, vocabularyItemRepository, userDictionaryService, new MediaResponseMapperImpl());
	}

	private static PronunciationRequest request(String word, String phrase) {
		return new PronunciationRequest(wordInfoId(), word, phrase, PronunciationRequest.LanguageEnum.DE);
	}

	private static PronunciationAsset completedVideoAsset() {
		PronunciationAsset asset = PronunciationAsset.queued(vocabularyItem(), "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "de", OffsetDateTime.now());
		asset.setId(UUID.randomUUID());
		asset.setStatus(PronunciationAssetStatus.COMPLETED);
		asset.setVideoObjectKey("pronunciations/%s/video.mp4".formatted(asset.getId()));
		asset.setCompletedAt(OffsetDateTime.now());
		return asset;
	}

	private static PronunciationAsset failedAsset() {
		PronunciationAsset asset = PronunciationAsset.queued(vocabularyItem(), "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "de", OffsetDateTime.now());
		asset.setId(UUID.randomUUID());
		asset.setStatus(PronunciationAssetStatus.FAILED);
		asset.setVideoObjectKey("pronunciations/%s/video.mp4".formatted(asset.getId()));
		asset.setErrorCode("video_provider_error");
		asset.setErrorMessage("Veo failed");
		return asset;
	}

	private static UUID wordInfoId() {
		return UUID.fromString("11111111-1111-1111-1111-111111111111");
	}

	private static VocabularyItem vocabularyItem() {
		VocabularyItem item = new VocabularyItem();
		item.setId(wordInfoId());
		item.setLanguage("de");
		item.setWord("Hausaufgabe");
		item.setPhrase("Ich mache meine Hausaufgabe nach dem Abendessen.");
		item.setPartOfSpeech("noun");
		item.setGender("feminine");
		item.setPlural("Hausaufgaben");
		item.setFrequency("common");
		item.setCompound(true);
		item.setCreatedAt(OffsetDateTime.now());
		item.setUpdatedAt(OffsetDateTime.now());
		return item;
	}

}
