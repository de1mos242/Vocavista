package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.PhraseImageRequest;
import com.vocavista.backend.api.model.PhraseImageStatus;
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
class PhraseImageServiceTest {

	@Mock
	private PhraseImageRepository phraseImageRepository;

	@Mock
	private PhraseImageGenerationProcessor generationProcessor;

	@Mock
	private MediaStorageService mediaStorageService;

	@Mock
	private VocabularyItemRepository vocabularyItemRepository;

	@Mock
	private UserDictionaryService userDictionaryService;

	@Test
	void createsQueuedAssetAndStartsGeneration() {
		when(phraseImageRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(), "Ich mache meine Hausaufgabe."))
				.thenReturn(Optional.empty());
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));
		when(phraseImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service().create(request());

		assertThat(response.getId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(PhraseImageStatus.QUEUED);
		verify(userDictionaryService).ensureEntryForCurrentUser(any(VocabularyItem.class));
		verify(generationProcessor).process(response.getId());
	}

	@Test
	void reusesExistingCachedCompletedImage() {
		PhraseImageAsset asset = completedImageAsset();
		when(phraseImageRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(), "Ich mache meine Hausaufgabe."))
				.thenReturn(Optional.of(asset));
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));

		var response = service().create(request());

		assertThat(response.getId()).isEqualTo(asset.getId());
		assertThat(response.getStatus()).isEqualTo(PhraseImageStatus.COMPLETED);
		assertThat(response.getImageUrl()).hasToString("/api/v1/media/phrase-images/" + asset.getId() + "/image");
		verify(userDictionaryService).ensureEntryForCurrentUser(any(VocabularyItem.class));
	}

	@Test
	void selectsGeneratedCandidate() {
		PhraseImageAsset asset = completedImageAsset();
		asset.setStatus(PhraseImageAssetStatus.AWAITING_SELECTION);
		asset.setImageObjectKey(null);
		asset.setImageCandidateCount(3);
		when(phraseImageRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(phraseImageRepository.save(asset)).thenReturn(asset);

		var response = service().selectCandidate(asset.getId(), 1);

		assertThat(asset.getStatus()).isEqualTo(PhraseImageAssetStatus.COMPLETED);
		assertThat(asset.getImageObjectKey()).endsWith("/candidates/1/image");
		assertThat(asset.getCompletedAt()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(PhraseImageStatus.COMPLETED);
		assertThat(response.getImageUrl()).hasToString("/api/v1/media/phrase-images/" + asset.getId() + "/image");
		verify(phraseImageRepository).save(asset);
	}

	@Test
	void doesNotChangeAlreadySelectedImage() {
		PhraseImageAsset asset = completedImageAsset();
		String selectedImageKey = asset.getImageObjectKey();
		asset.setImageCandidateCount(3);
		when(phraseImageRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

		var response = service().selectCandidate(asset.getId(), 1);

		assertThat(asset.getStatus()).isEqualTo(PhraseImageAssetStatus.COMPLETED);
		assertThat(asset.getImageObjectKey()).isEqualTo(selectedImageKey);
		assertThat(response.getStatus()).isEqualTo(PhraseImageStatus.COMPLETED);
		verify(phraseImageRepository).findById(asset.getId());
		verifyNoMoreInteractions(phraseImageRepository);
	}

	@Test
	void startsGenerationAfterCommitWhenTransactionSynchronizationIsActive() {
		when(phraseImageRepository.findByVocabularyItemIdAndPhraseIgnoreCase(wordInfoId(), "Ich mache meine Hausaufgabe."))
				.thenReturn(Optional.empty());
		when(vocabularyItemRepository.findById(wordInfoId())).thenReturn(Optional.of(vocabularyItem()));
		when(phraseImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		TransactionSynchronizationManager.initSynchronization();
		try {
			var response = service().create(request());

			verify(generationProcessor, never()).process(any());
			TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());

			verify(generationProcessor).process(response.getId());
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private PhraseImageService service() {
		return new PhraseImageService(phraseImageRepository, generationProcessor, mediaStorageService, vocabularyItemRepository,
				userDictionaryService, new MediaResponseMapperImpl());
	}

	private static PhraseImageRequest request() {
		return new PhraseImageRequest(wordInfoId(), "Hausaufgabe", "Ich mache meine Hausaufgabe.",
				PhraseImageRequest.LanguageEnum.DE);
	}

	private static PhraseImageAsset completedImageAsset() {
		PhraseImageAsset asset = PhraseImageAsset.queued(vocabularyItem(), "Hausaufgabe", "Ich mache meine Hausaufgabe.",
				"de", "v1", OffsetDateTime.now());
		asset.setStatus(PhraseImageAssetStatus.COMPLETED);
		asset.setImageObjectKey("phrase-images/%s/image.png".formatted(asset.getId()));
		asset.setCompletedAt(OffsetDateTime.now());
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
		item.setPhrase("Ich mache meine Hausaufgabe.");
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
