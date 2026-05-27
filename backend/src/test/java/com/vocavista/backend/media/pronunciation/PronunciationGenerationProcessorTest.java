package com.vocavista.backend.media.pronunciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.wordinfo.WordInfoRecord;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PronunciationGenerationProcessorTest {

	@Mock
	private PronunciationRepository pronunciationRepository;

	@Mock
	private PronunciationAudioGenerator pronunciationAudioGenerator;

	@Mock
	private PronunciationVideoGenerator pronunciationVideoGenerator;

	@Mock
	private MediaStorageService mediaStorageService;

	@Test
	void completesGenerationWithVeoVideoRenderMode() {
		PronunciationAsset asset = queuedAsset(PronunciationRenderMode.VEO_VIDEO, nounWordInfoJson("feminine", "die"));
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(pronunciationVideoGenerator.generate(any())).thenReturn(new GeneratedVideo("video".getBytes(), "video/mp4"));
		when(pronunciationVideoGenerator.providerName()).thenReturn("google-veo");
		when(pronunciationVideoGenerator.modelName()).thenReturn("veo-model");
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				pronunciationAudioGenerator, pronunciationVideoGenerator, mediaStorageService);

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationAssetStatus.COMPLETED);
		assertThat(asset.getVideoObjectKey()).endsWith("/video.mp4");
		assertThat(asset.getVideoProvider()).isEqualTo("google-veo");
		assertThat(asset.getCompletedAt()).isNotNull();
		ArgumentCaptor<PronunciationScript> script = ArgumentCaptor.forClass(PronunciationScript.class);
		verify(pronunciationVideoGenerator).generate(script.capture());
		assertThat(script.getValue().text()).isEqualTo("Hausaufgabe...\n\ndie Hausaufgabe!\n\nIch mache meine Hausaufgabe nach dem Abendessen.");
		assertThat(script.getValue().speakerDescription()).isEqualTo("female adult speaker");
		verify(mediaStorageService).store(eq(asset.getVideoObjectKey()), eq("video/mp4"), any(byte[].class));
	}

	@Test
	void completesGenerationWithTalkingHeadAudioRenderMode() {
		PronunciationAsset asset = queuedAsset(PronunciationRenderMode.TALKING_HEAD);
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(pronunciationAudioGenerator.generate(any())).thenReturn(new GeneratedAudio("audio".getBytes(), "audio/mpeg"));
		when(pronunciationAudioGenerator.providerName()).thenReturn("openai");
		when(pronunciationAudioGenerator.modelName()).thenReturn("model");
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				pronunciationAudioGenerator, pronunciationVideoGenerator, mediaStorageService);

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationAssetStatus.COMPLETED);
		assertThat(asset.getAudioObjectKey()).endsWith("/audio.mp3");
		assertThat(asset.getAudioProvider()).isEqualTo("openai");
		assertThat(asset.getCompletedAt()).isNotNull();
		verify(mediaStorageService).store(eq(asset.getAudioObjectKey()), eq("audio/mpeg"), any(byte[].class));
	}

	@Test
	void marksAssetFailedWhenProviderFails() {
		PronunciationAsset asset = queuedAsset(PronunciationRenderMode.TALKING_HEAD);
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(pronunciationAudioGenerator.generate(any()))
				.thenThrow(new MediaGenerationException("tts_error", "TTS failed"));
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				pronunciationAudioGenerator, pronunciationVideoGenerator, mediaStorageService);

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationAssetStatus.FAILED);
		assertThat(asset.getErrorCode()).isEqualTo("tts_error");
		assertThat(asset.getErrorMessage()).isEqualTo("TTS failed");
	}

	private static PronunciationAsset queuedAsset(PronunciationRenderMode renderMode) {
		return queuedAsset(renderMode, "{}");
	}

	private static PronunciationAsset queuedAsset(PronunciationRenderMode renderMode, String responseJson) {
		WordInfoRecord wordInfoRecord = new WordInfoRecord();
		wordInfoRecord.setId(UUID.randomUUID());
		wordInfoRecord.setResponseJson(responseJson);
		return PronunciationAsset.queued(wordInfoRecord, "Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.",
				"Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.", "de", renderMode.apiValue(), "hash",
				OffsetDateTime.now());
	}

	private static String nounWordInfoJson(String gender, String article) {
		return """
				{
				  "normalizedWord": "Hausaufgabe",
				  "language": "de",
				  "partOfSpeech": "noun",
				  "gender": "%s",
				  "article": "%s"
				}
				""".formatted(gender, article);
	}

}
