package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PronunciationGenerationProcessorTest {

	@Mock
	private PronunciationRepository pronunciationRepository;

	@Mock
	private TextToSpeechProvider textToSpeechProvider;

	@Mock
	private MediaStorageService mediaStorageService;

	@Test
	void completesGenerationWithAudioOnlyTalkingHeadRenderModeByDefault() {
		PronunciationAsset asset = queuedAsset();
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(textToSpeechProvider.generate(any())).thenReturn(new GeneratedAudio("audio".getBytes(), "audio/mpeg"));
		when(textToSpeechProvider.providerName()).thenReturn("elevenlabs");
		when(textToSpeechProvider.modelName()).thenReturn("model");
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				textToSpeechProvider, mediaStorageService);

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationAssetStatus.COMPLETED);
		assertThat(asset.getAudioObjectKey()).endsWith("/audio.mp3");
		assertThat(asset.getAudioProvider()).isEqualTo("elevenlabs");
		assertThat(asset.getCompletedAt()).isNotNull();
		verify(mediaStorageService).store(eq(asset.getAudioObjectKey()), eq("audio/mpeg"), any(byte[].class));
	}

	@Test
	void marksAssetFailedWhenProviderFails() {
		PronunciationAsset asset = queuedAsset();
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
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
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				failingProvider, mediaStorageService);

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationAssetStatus.FAILED);
		assertThat(asset.getErrorCode()).isEqualTo("tts_error");
		assertThat(asset.getErrorMessage()).isEqualTo("TTS failed");
	}

	private static PronunciationAsset queuedAsset() {
		return PronunciationAsset.queued("Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.",
				"Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
	}

}
