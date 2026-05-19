package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PronunciationVideoGenerationProcessorTest {

	@Mock
	private PronunciationVideoRepository pronunciationVideoRepository;

	@Test
	void completesGenerationWithFakeProvidersAndStorage() {
		PronunciationVideoAsset asset = queuedAsset();
		when(pronunciationVideoRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		PronunciationVideoGenerationProcessor processor = new PronunciationVideoGenerationProcessor(pronunciationVideoRepository,
				new FakeTextToSpeechProvider(), new FakeLipSyncVideoProvider(), new FakeMediaStorageService());

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationVideoAssetStatus.COMPLETED);
		assertThat(asset.getAudioObjectKey()).endsWith("/audio.txt");
		assertThat(asset.getVideoObjectKey()).endsWith("/video.txt");
		assertThat(asset.getAudioProvider()).isEqualTo("fake");
		assertThat(asset.getVideoProvider()).isEqualTo("fake");
		assertThat(asset.getCompletedAt()).isNotNull();
	}

	@Test
	void marksAssetFailedWhenProviderFails() {
		PronunciationVideoAsset asset = queuedAsset();
		when(pronunciationVideoRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		TextToSpeechProvider failingProvider = new TextToSpeechProvider() {
			@Override
			public GeneratedAudio generate(PronunciationScript script) {
				throw new MediaGenerationException("tts_error", "TTS failed");
			}

			@Override
			public String providerName() {
				return "test";
			}

			@Override
			public String modelName() {
				return "test";
			}
		};
		PronunciationVideoGenerationProcessor processor = new PronunciationVideoGenerationProcessor(pronunciationVideoRepository,
				failingProvider, new FakeLipSyncVideoProvider(), new FakeMediaStorageService());

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationVideoAssetStatus.FAILED);
		assertThat(asset.getErrorCode()).isEqualTo("tts_error");
		assertThat(asset.getErrorMessage()).isEqualTo("TTS failed");
	}

	private static PronunciationVideoAsset queuedAsset() {
		return PronunciationVideoAsset.queued("Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.",
				"Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
	}

}
