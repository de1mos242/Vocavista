package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.PronunciationVideoRequest;
import com.vocavista.backend.api.model.PronunciationVideoResponse;
import com.vocavista.backend.api.model.PronunciationVideoStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PronunciationVideoServiceTest {

	@Mock
	private PronunciationVideoRepository pronunciationVideoRepository;

	@Mock
	private PronunciationVideoGenerationProcessor generationProcessor;

	private final TextToSpeechProvider textToSpeechProvider = new FakeTextToSpeechProvider();
	private final MediaStorageService mediaStorageService = new FakeMediaStorageService();

	@Test
	void createsQueuedAssetAndStartsGeneration() {
		when(pronunciationVideoRepository.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(anyString(), anyString()))
				.thenReturn(Optional.empty());
		when(pronunciationVideoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		PronunciationVideoService service = service();

		PronunciationVideoResponse response = service.create(request(" Hausaufgabe ",
				" Ich mache meine Hausaufgabe nach dem Abendessen. "));

		assertThat(response.getId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(PronunciationVideoStatus.QUEUED);
		verify(generationProcessor).process(response.getId());
	}

	@Test
	void reusesExistingCachedAsset() {
		PronunciationVideoAsset existingAsset = completedAsset();
		when(pronunciationVideoRepository.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(anyString(), anyString()))
				.thenReturn(Optional.of(existingAsset));
		PronunciationVideoService service = service();

		PronunciationVideoResponse response = service.create(request("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen."));

		assertThat(response.getId()).isEqualTo(existingAsset.getId());
		assertThat(response.getStatus()).isEqualTo(PronunciationVideoStatus.COMPLETED);
		assertThat(response.getAudioUrl())
				.hasToString("/api/v1/media/pronunciation-videos/" + existingAsset.getId() + "/audio");
		assertThat(response.getRenderMode()).isEqualTo("talking-head");
		verify(pronunciationVideoRepository, never()).save(any());
		verify(generationProcessor, never()).process(any());
	}

	@Test
	void retriesExistingFailedAsset() {
		PronunciationVideoAsset existingAsset = failedAsset();
		when(pronunciationVideoRepository.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(anyString(), anyString()))
				.thenReturn(Optional.of(existingAsset));
		when(pronunciationVideoRepository.save(existingAsset)).thenReturn(existingAsset);
		PronunciationVideoService service = service();

		PronunciationVideoResponse response = service.create(request("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen."));

		assertThat(response.getId()).isEqualTo(existingAsset.getId());
		assertThat(response.getStatus()).isEqualTo(PronunciationVideoStatus.QUEUED);
		assertThat(response.getErrorCode()).isNull();
		assertThat(response.getErrorMessage()).isNull();
		assertThat(existingAsset.getErrorCode()).isNull();
		assertThat(existingAsset.getErrorMessage()).isNull();
		assertThat(existingAsset.getAudioObjectKey()).isNull();
		verify(pronunciationVideoRepository).save(existingAsset);
		verify(generationProcessor).process(existingAsset.getId());
	}

	@Test
	void rejectsBlankPhraseAfterTrimming() {
		PronunciationVideoService service = service();

		assertThatThrownBy(() -> service.create(request("Hausaufgabe", "   ")))
				.isInstanceOf(PronunciationVideoValidationException.class);
	}

	private PronunciationVideoService service() {
		return new PronunciationVideoService(pronunciationVideoRepository, generationProcessor, textToSpeechProvider,
				mediaStorageService);
	}

	private static PronunciationVideoRequest request(String word, String phrase) {
		return new PronunciationVideoRequest(word, phrase, PronunciationVideoRequest.LanguageEnum.DE);
	}

	private static PronunciationVideoAsset completedAsset() {
		PronunciationVideoAsset asset = PronunciationVideoAsset.queued("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
		asset.setId(UUID.randomUUID());
		asset.setStatus(PronunciationVideoAssetStatus.COMPLETED);
		asset.setAudioObjectKey("pronunciation-videos/%s/audio.txt".formatted(asset.getId()));
		asset.setCompletedAt(OffsetDateTime.now());
		return asset;
	}

	private static PronunciationVideoAsset failedAsset() {
		PronunciationVideoAsset asset = PronunciationVideoAsset.queued("Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "Hausaufgabe",
				"Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
		asset.setId(UUID.randomUUID());
		asset.setStatus(PronunciationVideoAssetStatus.FAILED);
		asset.setAudioObjectKey("pronunciation-videos/%s/audio.mp3".formatted(asset.getId()));
		asset.setErrorCode("tts_provider_error");
		asset.setErrorMessage("ElevenLabs failed");
		return asset;
	}

}
