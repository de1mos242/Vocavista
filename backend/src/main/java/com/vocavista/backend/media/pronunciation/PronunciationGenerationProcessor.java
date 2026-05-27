package com.vocavista.backend.media.pronunciation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordInfoResponse;
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
	private final PronunciationAudioGenerator pronunciationAudioGenerator;
	private final PronunciationVideoGenerator pronunciationVideoGenerator;
	private final MediaStorageService mediaStorageService;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.script-template-version:v6}")
	private String scriptTemplateVersion = "v6";

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
			PronunciationRenderMode renderMode = PronunciationRenderMode.fromApiValue(asset.getRenderMode());
			if (renderMode == PronunciationRenderMode.VEO_VIDEO) {
				generateVideo(asset, script);
			}
			else {
				generateAudio(asset, script);
			}
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

	private void generateVideo(PronunciationAsset asset, PronunciationScript script) {
		GeneratedVideo video = pronunciationVideoGenerator.generate(script);
		String videoObjectKey = "pronunciations/" + asset.getId() + "/video." + extensionFor(video.contentType());
		mediaStorageService.store(videoObjectKey, video.contentType(), video.bytes());
		asset.setVideoObjectKey(videoObjectKey);
		asset.setVideoProvider(pronunciationVideoGenerator.providerName());
		asset.setVideoModel(pronunciationVideoGenerator.modelName());
	}

	private void generateAudio(PronunciationAsset asset, PronunciationScript script) {
		GeneratedAudio audio = pronunciationAudioGenerator.generate(script);
		String audioObjectKey = "pronunciations/" + asset.getId() + "/audio."
				+ extensionFor(audio.contentType());
		mediaStorageService.store(audioObjectKey, audio.contentType(), audio.bytes());
		asset.setAudioObjectKey(audioObjectKey);
		asset.setAudioProvider(pronunciationAudioGenerator.providerName());
		asset.setAudioModel(pronunciationAudioGenerator.modelName());
	}

	private void markFailed(PronunciationAsset asset, String code, String message, RuntimeException ex) {
		log.warn("Pronunciation media generation failed for {}", asset.getId(), ex);
		asset.setStatus(PronunciationAssetStatus.FAILED);
		asset.setErrorCode(code);
		asset.setErrorMessage(message);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
	}

	private PronunciationScript scriptFor(PronunciationAsset asset) {
		WordInfoMetadata metadata = wordInfoMetadata(asset);
		String repeatedWord = metadata.article() == null
				? asset.getNormalizedWord()
				: metadata.article() + " " + asset.getNormalizedWord();
		String text = "%s...\n\n%s!\n\n%s".formatted(asset.getNormalizedWord(), repeatedWord,
				punctuated(asset.getNormalizedPhrase()));
		return new PronunciationScript(asset.getNormalizedWord(), asset.getNormalizedPhrase(), asset.getLanguage(), text,
				scriptTemplateVersion, voiceConfig, metadata.speakerDescription());
	}

	private WordInfoMetadata wordInfoMetadata(PronunciationAsset asset) {
		try {
			String responseJson = asset.getWordInfoRecord().getResponseJson();
			if (responseJson == null || responseJson.isBlank()) {
				return WordInfoMetadata.defaultMetadata();
			}
			WordInfoResponse wordInfo = objectMapper.readValue(responseJson, WordInfoResponse.class);
			String article = wordInfo.getPartOfSpeech() == PartOfSpeech.NOUN && wordInfo.getArticle() != null
					? wordInfo.getArticle().getValue()
					: null;
			return new WordInfoMetadata(article, speakerDescription(wordInfo.getGender()));
		}
		catch (JsonProcessingException | IllegalArgumentException ex) {
			log.warn("Could not read word info metadata for pronunciation script", ex);
			return WordInfoMetadata.defaultMetadata();
		}
	}

	private static String speakerDescription(Gender gender) {
		return switch (gender) {
			case MASCULINE -> "male adult speaker";
			case FEMININE -> "female adult speaker";
			case NEUTER -> "gender-neutral adult speaker";
			case null -> "adult German speaker";
		};
	}

	private static String punctuated(String value) {
		return value.endsWith(".") || value.endsWith("!") || value.endsWith("?") ? value : value + ".";
	}

	private static String extensionFor(String contentType) {
		return switch (contentType) {
			case "audio/aac" -> "aac";
			case "audio/flac" -> "flac";
			case "audio/mpeg" -> "mp3";
			case "audio/opus" -> "opus";
			case "audio/pcm" -> "pcm";
			case "audio/wav" -> "wav";
			case "video/mp4" -> "mp4";
			case "video/mpeg" -> "mpeg";
			case "video/quicktime" -> "mov";
			case "text/plain" -> "txt";
			default -> "bin";
		};
	}

	private record WordInfoMetadata(String article, String speakerDescription) {

		static WordInfoMetadata defaultMetadata() {
			return new WordInfoMetadata(null, "adult German speaker");
		}

	}

}
