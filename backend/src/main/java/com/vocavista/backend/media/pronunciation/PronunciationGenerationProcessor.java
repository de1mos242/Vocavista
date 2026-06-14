package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.wordinfo.WordInfoMetadataReader;
import com.vocavista.backend.wordinfo.WordInfoMetadataReader.PronunciationMetadata;
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
	private final PronunciationVideoGenerator pronunciationVideoGenerator;
	private final PronunciationVideoCompressor pronunciationVideoCompressor;
	private final MediaStorageService mediaStorageService;
	private final WordInfoMetadataReader wordInfoMetadataReader;
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.script-template-version:v6}")
	private String scriptTemplateVersion = "v6";

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
			generateVideo(asset, script);
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
		asset.setSmallVideoObjectKey(null);
		pronunciationVideoCompressor.compress(video).ifPresent(smallVideo -> {
			String smallVideoObjectKey = "pronunciations/" + asset.getId() + "/video-small.mp4";
			mediaStorageService.store(smallVideoObjectKey, smallVideo.contentType(), smallVideo.bytes());
			asset.setSmallVideoObjectKey(smallVideoObjectKey);
		});
		asset.setVideoProvider(pronunciationVideoGenerator.providerName());
		asset.setVideoModel(pronunciationVideoGenerator.modelName());
	}

	private void markFailed(PronunciationAsset asset, String code, String message, RuntimeException ex) {
		log.warn("Pronunciation media generation failed for {}", asset.getId(), ex);
		asset.setStatus(PronunciationAssetStatus.FAILED);
		asset.setErrorCode(code);
		asset.setErrorMessage(message);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
	}

	private PronunciationScript scriptFor(PronunciationAsset asset) {
		PronunciationMetadata metadata = wordInfoMetadataReader.pronunciationMetadata(asset.getWordInfoRecord());
		String repeatedWord = metadata.article() == null
				? asset.getNormalizedWord()
				: metadata.article() + " " + asset.getNormalizedWord();
		String text = "%s...\n\n%s!\n\n%s".formatted(asset.getNormalizedWord(), repeatedWord,
				punctuated(asset.getNormalizedPhrase()));
		return new PronunciationScript(asset.getNormalizedWord(), asset.getNormalizedPhrase(), asset.getLanguage(), text,
				scriptTemplateVersion, metadata.speakerDescription());
	}

	private static String punctuated(String value) {
		return value.endsWith(".") || value.endsWith("!") || value.endsWith("?") ? value : value + ".";
	}

	private static String extensionFor(String contentType) {
		return switch (contentType) {
			case "video/mp4" -> "mp4";
			case "video/mpeg" -> "mpeg";
			case "video/quicktime" -> "mov";
			default -> "bin";
		};
	}

}

record PronunciationScript(
		String word,
		String phrase,
		String language,
		String text,
		String templateVersion,
		String speakerDescription) {
}
