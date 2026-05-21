package com.vocavista.backend.media.pronunciation;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class PronunciationGenerationProcessor {

	private final PronunciationRepository pronunciationRepository;
	private final TextToSpeechProvider textToSpeechProvider;
	private final MediaStorageService mediaStorageService;
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.script-template-version:v1}")
	private String scriptTemplateVersion = "v1";

	@Value("${vocavista.media.voice-config:default-clear-german}")
	private String voiceConfig = "default-clear-german";

	@Async
	@Transactional
	public void process(UUID id) {
		PronunciationAsset asset = pronunciationRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Pronunciation asset was not found"));
		try {
			OffsetDateTime now = OffsetDateTime.now(clock);
			asset.setStatus(PronunciationAssetStatus.PROCESSING);
			asset.setUpdatedAt(now);

			PronunciationScript script = scriptFor(asset);
			GeneratedAudio audio = textToSpeechProvider.generate(script);
			String audioObjectKey = "pronunciations/" + asset.getId() + "/audio."
					+ extensionFor(audio.contentType());
			mediaStorageService.store(audioObjectKey, audio.contentType(), audio.bytes());

			asset.setAudioObjectKey(audioObjectKey);
			asset.setAudioProvider(textToSpeechProvider.providerName());
			asset.setAudioModel(textToSpeechProvider.modelName());
			asset.setStatus(PronunciationAssetStatus.COMPLETED);
			asset.setUpdatedAt(OffsetDateTime.now(clock));
			asset.setCompletedAt(asset.getUpdatedAt());
		}
		catch (MediaGenerationException ex) {
			markFailed(asset, ex.getCode(), ex.getMessage(), ex);
		}
		catch (RuntimeException ex) {
			markFailed(asset, "generation_error", "Could not generate pronunciation media", ex);
		}
	}

	private void markFailed(PronunciationAsset asset, String code, String message, RuntimeException ex) {
		log.warn("Pronunciation media generation failed for {}", asset.getId(), ex);
		asset.setStatus(PronunciationAssetStatus.FAILED);
		asset.setErrorCode(code);
		asset.setErrorMessage(message);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
	}

	private PronunciationScript scriptFor(PronunciationAsset asset) {
		String text = "%s...\n\n%s!\n\n%s".formatted(asset.getNormalizedWord(), asset.getNormalizedWord(),
				punctuated(asset.getNormalizedPhrase()));
		return new PronunciationScript(asset.getNormalizedWord(), asset.getNormalizedPhrase(), asset.getLanguage(), text,
				scriptTemplateVersion, voiceConfig);
	}

	private static String punctuated(String value) {
		return value.endsWith(".") || value.endsWith("!") || value.endsWith("?") ? value : value + ".";
	}

	private static String extensionFor(String contentType) {
		return switch (contentType) {
			case "audio/mpeg" -> "mp3";
			case "audio/wav" -> "wav";
			case "text/plain" -> "txt";
			default -> "bin";
		};
	}

}
