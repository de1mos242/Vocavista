package com.vocavista.backend.media.pronunciation;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
class PhraseImageGenerationProcessor {

	private final PhraseImageRepository phraseImageRepository;
	private final PhraseImageSceneDescriber phraseImageSceneDescriber;
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

			String sceneDescription = phraseImageSceneDescriber.describe(asset.getNormalizedWord(), asset.getNormalizedPhrase(),
					asset.getLanguage());
			PhraseImagePrompt prompt = promptFor(asset, sceneDescription);
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

	static PhraseImagePrompt promptFor(PhraseImageAsset asset, String sceneDescription) {
		String phrase = asset.getNormalizedPhrase().replaceAll("\\s+", " ").trim();
		String word = asset.getNormalizedWord().replaceAll("\\s+", " ").trim();
		String safeSceneDescription = stripLiteralInput(sceneDescription, phrase, word);
		String text = """
				Create a cinematic 16:9 high-quality realistic everyday scene.
				Scene description: %s.
				Build a natural, memorable composition using physical objects, people, actions, environment, lighting, color, focus, and framing.
				Use clean cinematic composition, polished realistic details, natural anatomy, expressive faces, and plausible everyday objects.
				""".formatted(safeSceneDescription).replaceAll("\\s+", " ").trim();
		return new PhraseImagePrompt(word, phrase, asset.getLanguage(), text, asset.getPromptVersion());
	}

	private static String stripLiteralInput(String sceneDescription, String phrase, String word) {
		String safeDescription = sceneDescription == null ? "" : sceneDescription;
		for (String literal : new String[] { phrase, word }) {
			if (StringUtils.hasText(literal)) {
				safeDescription = Pattern.compile(Pattern.quote(literal), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
						.matcher(safeDescription)
						.replaceAll("");
			}
		}
		String phraseWithoutTrailingPunctuation = phrase.replaceAll("[\\p{Punct}\\s]+$", "");
		if (StringUtils.hasText(phraseWithoutTrailingPunctuation)) {
			safeDescription = Pattern
					.compile(Pattern.quote(phraseWithoutTrailingPunctuation), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
					.matcher(safeDescription)
					.replaceAll("");
		}
		for (String token : phrase.split("\\s+")) {
			String cleanToken = token.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
			if (cleanToken.length() > 2) {
				safeDescription = Pattern
						.compile("(?<!\\p{L})" + Pattern.quote(cleanToken) + "(?!\\p{L})",
								Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
						.matcher(safeDescription)
						.replaceAll("");
			}
		}
		safeDescription = safeDescription.replaceAll("\\s+", " ").trim();
		if (StringUtils.hasText(safeDescription)) {
			return safeDescription;
		}
		return "A person in a natural everyday learning-related situation, shown through concrete objects, actions, setting, lighting, and composition";
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
