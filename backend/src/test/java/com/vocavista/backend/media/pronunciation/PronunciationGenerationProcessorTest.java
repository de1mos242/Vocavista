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
	private PronunciationVideoGenerator pronunciationVideoGenerator;

	@Mock
	private MediaStorageService mediaStorageService;

	@Test
	void completesGenerationWithVeoVideo() {
		PronunciationAsset asset = queuedAsset(nounWordInfoJson("feminine", "die"));
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(pronunciationVideoGenerator.generate(any())).thenReturn(new GeneratedVideo("video".getBytes(), "video/mp4"));
		when(pronunciationVideoGenerator.providerName()).thenReturn("google-veo");
		when(pronunciationVideoGenerator.modelName()).thenReturn("veo-model");
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				pronunciationVideoGenerator, mediaStorageService);

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
	void marksAssetFailedWhenProviderFails() {
		PronunciationAsset asset = queuedAsset();
		when(pronunciationRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
		when(pronunciationVideoGenerator.generate(any()))
				.thenThrow(new MediaGenerationException("video_provider_error", "Veo failed"));
		PronunciationGenerationProcessor processor = new PronunciationGenerationProcessor(pronunciationRepository,
				pronunciationVideoGenerator, mediaStorageService);

		processor.process(asset.getId());

		assertThat(asset.getStatus()).isEqualTo(PronunciationAssetStatus.FAILED);
		assertThat(asset.getErrorCode()).isEqualTo("video_provider_error");
		assertThat(asset.getErrorMessage()).isEqualTo("Veo failed");
	}

	private static PronunciationAsset queuedAsset() {
		return queuedAsset("{}");
	}

	private static PronunciationAsset queuedAsset(String responseJson) {
		WordInfoRecord wordInfoRecord = new WordInfoRecord();
		wordInfoRecord.setId(UUID.randomUUID());
		wordInfoRecord.setResponseJson(responseJson);
		return PronunciationAsset.queued(wordInfoRecord, "Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.",
				"Hausaufgabe", "Ich mache meine Hausaufgabe nach dem Abendessen.", "de", "hash", OffsetDateTime.now());
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
