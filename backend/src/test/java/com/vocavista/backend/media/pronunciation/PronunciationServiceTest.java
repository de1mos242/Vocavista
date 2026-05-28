package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.PronunciationRequest;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.dictionary.UserDictionaryService;
import com.vocavista.backend.wordinfo.WordInfoRecord;
import com.vocavista.backend.wordinfo.WordInfoRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PronunciationServiceTest {

	@Mock
	private PronunciationRepository pronunciationRepository;

	@Mock
	private PronunciationGenerationProcessor generationProcessor;

	@Mock
	private PronunciationVideoGenerator pronunciationVideoGenerator;

	@Mock
	private MediaStorageService mediaStorageService;

	@Mock
	private WordInfoRepository wordInfoRepository;

	@Mock
	private UserDictionaryService userDictionaryService;

	@Test
	void createsQueuedAssetAndStartsGeneration() {
		when(pronunciationVideoGenerator.providerName()).thenReturn("google-veo");
		when(pronunciationVideoGenerator.modelName()).thenReturn("veo-model");
		when(pronunciationRepository.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(anyString(), anyString()))
				.thenReturn(Optional.empty());
		when(wordInfoRepository.findById(wordInfoId())).thenReturn(Optional.of(wordInfoRecord()));
		when(pronunciationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		PronunciationService service = service();

		PronunciationResponse response = service.create(request(" Hausaufgabe ",
				" Ich mache meine Hausaufgabe nach dem Abendessen. "));

		assertThat(response.getId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(PronunciationStatus.QUEUED);
		verify(userDictionaryService).ensureEntryForCurrentUser(any());
		verify(generationProcessor).process(response.getId());
	}

	@Test
	void reusesExistingCachedVideoAsset() {
		PronunciationAsset existingAsset = completedVideoAsset();
		when(pronunciationVideoGenerator.providerName()).thenReturn("google-veo");
		when(pronunciationVideoGenerator.modelName()).thenReturn("veo-model");
		when(pronunciationRepository.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(anyString(), anyString()))
				.thenReturn(Optional.of(existingAsset));
		when(wordInfoRepository.findById(wordInfoId())).thenReturn(Optional.of(wordInfoRecord()));
		PronunciationService service = service();

		PronunciationResponse response = service.create(request("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen."));

		assertThat(response.getId()).isEqualTo(existingAsset.getId());
		assertThat(response.getStatus()).isEqualTo(PronunciationStatus.COMPLETED);
		assertThat(response.getVideoUrl())
				.hasToString("/api/v1/media/pronunciations/" + existingAsset.getId() + "/video");
		verify(userDictionaryService).ensureEntryForCurrentUser(any());
		verify(pronunciationRepository, never()).save(any());
		verify(generationProcessor, never()).process(any());
	}

	@Test
	void retriesExistingFailedAsset() {
		PronunciationAsset existingAsset = failedAsset();
		when(pronunciationVideoGenerator.providerName()).thenReturn("google-veo");
		when(pronunciationVideoGenerator.modelName()).thenReturn("veo-model");
		when(pronunciationRepository.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(anyString(), anyString()))
				.thenReturn(Optional.of(existingAsset));
		when(wordInfoRepository.findById(wordInfoId())).thenReturn(Optional.of(wordInfoRecord()));
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
		assertThat(existingAsset.getVideoObjectKey()).isNull();
		verify(pronunciationRepository).save(existingAsset);
		verify(generationProcessor).process(existingAsset.getId());
	}

	@Test
	void rejectsBlankPhraseAfterTrimming() {
		PronunciationService service = service();

		assertThatThrownBy(() -> service.create(request("Hausaufgabe", "   ")))
				.isInstanceOf(PronunciationValidationException.class);
	}

	private PronunciationService service() {
		return new PronunciationService(pronunciationRepository, generationProcessor, pronunciationVideoGenerator,
				mediaStorageService, wordInfoRepository, userDictionaryService);
	}

	private static PronunciationRequest request(String word, String phrase) {
		return new PronunciationRequest(wordInfoId(), word, phrase, PronunciationRequest.LanguageEnum.DE);
	}

	private static PronunciationAsset completedVideoAsset() {
		PronunciationAsset asset = PronunciationAsset.queued(wordInfoRecord(), "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
		asset.setId(UUID.randomUUID());
		asset.setStatus(PronunciationAssetStatus.COMPLETED);
		asset.setVideoObjectKey("pronunciations/%s/video.mp4".formatted(asset.getId()));
		asset.setCompletedAt(OffsetDateTime.now());
		return asset;
	}

	private static PronunciationAsset failedAsset() {
		PronunciationAsset asset = PronunciationAsset.queued(wordInfoRecord(), "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
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

	private static WordInfoRecord wordInfoRecord() {
		WordInfoRecord record = new WordInfoRecord();
		record.setId(wordInfoId());
		record.setNormalizedQuery("hausaufgabe");
		record.setNormalizedWord("Hausaufgabe");
		record.setLanguage("de");
		record.setResponseJson("{}");
		record.setCreatedAt(OffsetDateTime.now());
		record.setUpdatedAt(OffsetDateTime.now());
		return record;
	}

}
