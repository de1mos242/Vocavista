package com.vocavista.backend.media.pronunciation;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
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
	private final SpringAiPhraseImageSceneDescriber phraseImageSceneDescriber;
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
			generateImages(asset, prompt);
			asset.setUpdatedAt(OffsetDateTime.now(clock));
			if (asset.getStatus() == PhraseImageAssetStatus.COMPLETED) {
				asset.setCompletedAt(asset.getUpdatedAt());
			}
		}
		catch (MediaGenerationException ex) {
			markFailed(asset, ex.getCode(), ex.getMessage(), ex);
		}
		catch (RuntimeException ex) {
			markFailed(asset, "generation_error", "Could not generate phrase image", ex);
		}
	}

	private void generateImages(PhraseImageAsset asset, PhraseImagePrompt prompt) {
		List<GeneratedImage> images = phraseImageGenerator.generate(prompt);
		for (int i = 0; i < images.size(); i += 1) {
			GeneratedImage image = images.get(i);
			mediaStorageService.store(PhraseImageService.candidateObjectKey(asset.getId(), i), image.contentType(), image.bytes());
		}
		asset.setImageCandidateCount(images.size());
		asset.setImageProvider(phraseImageGenerator.providerName());
		asset.setImageModel(phraseImageGenerator.modelName());
		if (images.size() == 1) {
			asset.setImageObjectKey(PhraseImageService.candidateObjectKey(asset.getId(), 0));
			asset.setStatus(PhraseImageAssetStatus.COMPLETED);
			return;
		}
		asset.setImageObjectKey(null);
		asset.setStatus(PhraseImageAssetStatus.AWAITING_SELECTION);
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
		String cleanSceneDescription = sceneDescription.replaceAll("\\s+", " ").trim();
		String text = """
				Create a high-quality 16:9 image of this scene: %s.
				""".formatted(cleanSceneDescription).replaceAll("\\s+", " ").trim();
		return new PhraseImagePrompt(word, phrase, asset.getLanguage(), text, asset.getPromptVersion());
	}

}
