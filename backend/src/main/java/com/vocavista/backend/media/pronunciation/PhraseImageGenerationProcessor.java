package com.vocavista.backend.media.pronunciation;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class PhraseImageGenerationProcessor {

	private final PhraseImageRepository phraseImageRepository;
	private final PhraseImageGenerator phraseImageGenerator;
	private final MediaStorageService mediaStorageService;
	private final Clock clock = Clock.systemUTC();

	@Async
	@Transactional
	public void process(UUID id) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		try {
			OffsetDateTime now = OffsetDateTime.now(clock);
			asset.setStatus(PhraseImageAssetStatus.PROCESSING);
			asset.setUpdatedAt(now);

			PhraseImagePrompt prompt = promptFor(asset);
			asset.setPromptText(prompt.text());
			generateImage(asset, prompt);
			asset.setStatus(PhraseImageAssetStatus.COMPLETED);
			asset.setUpdatedAt(OffsetDateTime.now(clock));
			asset.setCompletedAt(asset.getUpdatedAt());
		}
		catch (MediaGenerationException ex) {
			markFailed(asset, ex.getCode(), ex.getMessage(), ex);
		}
		catch (RuntimeException ex) {
			markFailed(asset, "generation_error", "Could not generate phrase image", ex);
		}
	}

	private void generateImage(PhraseImageAsset asset, PhraseImagePrompt prompt) {
		GeneratedImage image = phraseImageGenerator.generate(prompt);
		String imageObjectKey = "phrase-images/" + asset.getId() + "/image." + extensionFor(image.contentType());
		mediaStorageService.store(imageObjectKey, image.contentType(), image.bytes());
		asset.setImageObjectKey(imageObjectKey);
		asset.setImageProvider(phraseImageGenerator.providerName());
		asset.setImageModel(phraseImageGenerator.modelName());
	}

	private void markFailed(PhraseImageAsset asset, String code, String message, RuntimeException ex) {
		log.warn("Phrase image generation failed for {}", asset.getId(), ex);
		asset.setStatus(PhraseImageAssetStatus.FAILED);
		asset.setErrorCode(code);
		asset.setErrorMessage(message);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
	}

	static PhraseImagePrompt promptFor(PhraseImageAsset asset) {
		String phrase = asset.getNormalizedPhrase().replaceAll("\\s+", " ").trim();
		String word = asset.getNormalizedWord().replaceAll("\\s+", " ").trim();
		String text = """
				Create a cinematic 16:9 high-quality educational image for a German vocabulary card.
				Phrase context: "%s".
				Vocabulary concept: "%s".
				Build a natural, realistic, memorable scene for language learning.
				Communicate the vocabulary concept with physical objects, people, actions, environment, lighting, color, focus, and framing.
				Use clean cinematic composition, polished realistic details, natural anatomy, expressive faces, and plausible everyday objects.
				""".formatted(phrase, word).replaceAll("\\s+", " ").trim();
		return new PhraseImagePrompt(word, phrase, asset.getLanguage(), text, asset.getPromptVersion());
	}

	private static String extensionFor(String contentType) {
		return switch (contentType) {
			case "image/jpeg" -> "jpg";
			case "image/webp" -> "webp";
			case "image/png" -> "png";
			default -> "bin";
		};
	}

}
