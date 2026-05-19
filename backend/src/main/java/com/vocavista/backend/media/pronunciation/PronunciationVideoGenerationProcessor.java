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
class PronunciationVideoGenerationProcessor {

	private final PronunciationVideoRepository pronunciationVideoRepository;
	private final TextToSpeechProvider textToSpeechProvider;
	private final LipSyncVideoProvider lipSyncVideoProvider;
	private final MediaStorageService mediaStorageService;
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.script-template-version:v1}")
	private String scriptTemplateVersion = "v1";

	@Value("${vocavista.media.voice-config:default-clear-german}")
	private String voiceConfig = "default-clear-german";

	@Value("${vocavista.media.avatar-config:default-talking-head}")
	private String avatarConfig = "default-talking-head";

	@Async
	@Transactional
	public void process(UUID id) {
		PronunciationVideoAsset asset = pronunciationVideoRepository.findById(id)
				.orElseThrow(() -> new PronunciationVideoNotFoundException("Pronunciation video asset was not found"));
		try {
			OffsetDateTime now = OffsetDateTime.now(clock);
			asset.setStatus(PronunciationVideoAssetStatus.PROCESSING);
			asset.setUpdatedAt(now);

			PronunciationScript script = scriptFor(asset);
			GeneratedAudio audio = textToSpeechProvider.generate(script);
			String audioObjectKey = "pronunciation-videos/" + asset.getId() + "/audio.txt";
			mediaStorageService.store(audioObjectKey, audio.contentType(), audio.bytes());

			GeneratedVideo video = lipSyncVideoProvider.generate(script, audio);
			String videoObjectKey = "pronunciation-videos/" + asset.getId() + "/video.txt";
			mediaStorageService.store(videoObjectKey, video.contentType(), video.bytes());

			asset.setAudioObjectKey(audioObjectKey);
			asset.setVideoObjectKey(videoObjectKey);
			asset.setAudioProvider(textToSpeechProvider.providerName());
			asset.setAudioModel(textToSpeechProvider.modelName());
			asset.setVideoProvider(lipSyncVideoProvider.providerName());
			asset.setVideoModel(lipSyncVideoProvider.modelName());
			asset.setStatus(PronunciationVideoAssetStatus.COMPLETED);
			asset.setUpdatedAt(OffsetDateTime.now(clock));
			asset.setCompletedAt(asset.getUpdatedAt());
		}
		catch (MediaGenerationException ex) {
			markFailed(asset, ex.getCode(), ex.getMessage(), ex);
		}
		catch (RuntimeException ex) {
			markFailed(asset, "generation_error", "Could not generate pronunciation video", ex);
		}
	}

	private void markFailed(PronunciationVideoAsset asset, String code, String message, RuntimeException ex) {
		log.warn("Pronunciation video generation failed for {}", asset.getId(), ex);
		asset.setStatus(PronunciationVideoAssetStatus.FAILED);
		asset.setErrorCode(code);
		asset.setErrorMessage(message);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
	}

	private PronunciationScript scriptFor(PronunciationVideoAsset asset) {
		String text = ("Say the German word \"%s\" twice, with a short pause between repetitions. "
				+ "Use a neutral clear pronunciation for the first repetition and a slightly more natural conversational "
				+ "intonation for the second repetition. After another short pause, say the phrase: \"%s\"")
				.formatted(asset.getNormalizedWord(), asset.getNormalizedPhrase());
		return new PronunciationScript(asset.getNormalizedWord(), asset.getNormalizedPhrase(), asset.getLanguage(), text,
				scriptTemplateVersion, voiceConfig, avatarConfig);
	}

}
