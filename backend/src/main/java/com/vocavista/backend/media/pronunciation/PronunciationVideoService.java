package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PronunciationVideoRequest;
import com.vocavista.backend.api.model.PronunciationVideoResponse;
import com.vocavista.backend.api.model.PronunciationVideoStatus;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class PronunciationVideoService {

	private static final int MAX_WORD_LENGTH = 80;
	private static final int MAX_PHRASE_LENGTH = 240;
	private static final String SUPPORTED_LANGUAGE = "de";

	private final PronunciationVideoRepository pronunciationVideoRepository;
	private final PronunciationVideoGenerationProcessor generationProcessor;
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

	@Value("${vocavista.media.render-mode:talking-head}")
	private String renderMode = "talking-head";

	PronunciationVideoResponse create(PronunciationVideoRequest request) {
		NormalizedInput input = normalize(request);
		String contentHash = contentHash(input);

		return pronunciationVideoRepository
				.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(input.language(), contentHash)
				.map(this::reuseOrRetry)
				.orElseGet(() -> createQueuedAsset(input, contentHash));
	}

	PronunciationVideoResponse get(UUID id) {
		PronunciationVideoAsset asset = pronunciationVideoRepository.findById(id)
				.orElseThrow(() -> new PronunciationVideoNotFoundException("Pronunciation video asset was not found"));
		return toResponse(asset);
	}

	StoredMedia getAudio(UUID id) {
		PronunciationVideoAsset asset = pronunciationVideoRepository.findById(id)
				.orElseThrow(() -> new PronunciationVideoNotFoundException("Pronunciation video asset was not found"));
		if (asset.getStatus() != PronunciationVideoAssetStatus.COMPLETED || !StringUtils.hasText(asset.getAudioObjectKey())) {
			throw new PronunciationVideoNotFoundException("Pronunciation audio was not found");
		}
		return mediaStorageService.read(asset.getAudioObjectKey());
	}

	private PronunciationVideoResponse createQueuedAsset(NormalizedInput input, String contentHash) {
		PronunciationVideoAsset asset = PronunciationVideoAsset.queued(input.word(), input.phrase(), input.normalizedWord(),
				input.normalizedPhrase(), input.language(), contentHash, OffsetDateTime.now(clock));
		try {
			PronunciationVideoAsset savedAsset = pronunciationVideoRepository.save(asset);
			generationProcessor.process(savedAsset.getId());
			return toResponse(savedAsset);
		}
		catch (DataIntegrityViolationException ex) {
			return pronunciationVideoRepository
					.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(input.language(), contentHash)
					.map(this::reuseOrRetry)
					.orElseThrow(() -> ex);
		}
	}

	private PronunciationVideoResponse reuseOrRetry(PronunciationVideoAsset asset) {
		if (asset.getStatus() != PronunciationVideoAssetStatus.FAILED) {
			return toResponse(asset);
		}

		asset.setStatus(PronunciationVideoAssetStatus.QUEUED);
		asset.setAudioObjectKey(null);
		asset.setVideoObjectKey(null);
		asset.setAudioProvider(null);
		asset.setAudioModel(null);
		asset.setVideoProvider(null);
		asset.setVideoModel(null);
		asset.setErrorCode(null);
		asset.setErrorMessage(null);
		asset.setCompletedAt(null);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		PronunciationVideoAsset savedAsset = pronunciationVideoRepository.save(asset);
		generationProcessor.process(savedAsset.getId());
		return toResponse(savedAsset);
	}

	private PronunciationVideoResponse toResponse(PronunciationVideoAsset asset) {
		PronunciationVideoResponse response = new PronunciationVideoResponse(asset.getId(),
				PronunciationVideoStatus.fromValue(asset.getStatus().name().toLowerCase()));
		response.setRenderMode(renderMode);
		if (asset.getStatus() == PronunciationVideoAssetStatus.COMPLETED && StringUtils.hasText(asset.getAudioObjectKey())) {
			if ("talking-head".equals(renderMode)) {
				response.setAudioUrl(URI.create("/api/v1/media/pronunciation-videos/" + asset.getId() + "/audio"));
			}
			else {
				PlayableMedia playableMedia = mediaStorageService.playableUrl(asset.getAudioObjectKey());
				response.setAudioUrl(playableMedia.url());
				response.setExpiresAt(playableMedia.expiresAt());
			}
		}
		if (asset.getStatus() == PronunciationVideoAssetStatus.COMPLETED && StringUtils.hasText(asset.getVideoObjectKey())) {
			PlayableMedia playableMedia = mediaStorageService.playableUrl(asset.getVideoObjectKey());
			response.setVideoUrl(playableMedia.url());
			response.setExpiresAt(playableMedia.expiresAt());
		}
		if (asset.getStatus() == PronunciationVideoAssetStatus.FAILED) {
			response.setErrorCode(asset.getErrorCode());
			response.setErrorMessage(asset.getErrorMessage());
		}
		return response;
	}

	private NormalizedInput normalize(PronunciationVideoRequest request) {
		if (request == null) {
			throw new PronunciationVideoValidationException("request body is required");
		}

		String word = trimAndCollapse(request.getWord());
		String phrase = trimAndCollapse(request.getPhrase());
		String language = request.getLanguage() == null ? "" : request.getLanguage().toString();
		if (!StringUtils.hasText(word)) {
			throw new PronunciationVideoValidationException("word must not be blank");
		}
		if (word.length() > MAX_WORD_LENGTH) {
			throw new PronunciationVideoValidationException("word must not exceed 80 characters");
		}
		if (!StringUtils.hasText(phrase)) {
			throw new PronunciationVideoValidationException("phrase must not be blank");
		}
		if (phrase.length() > MAX_PHRASE_LENGTH) {
			throw new PronunciationVideoValidationException("phrase must not exceed 240 characters");
		}
		if (!SUPPORTED_LANGUAGE.equals(language)) {
			throw new PronunciationVideoValidationException("only German language code de is supported");
		}
		return new NormalizedInput(request.getWord(), request.getPhrase(), word, phrase, language);
	}

	private String contentHash(NormalizedInput input) {
		String value = String.join("\n", input.language(), input.normalizedWord().toLowerCase(),
				input.normalizedPhrase().toLowerCase(), scriptTemplateVersion, voiceConfig, avatarConfig, renderMode,
				textToSpeechProvider.providerName(), textToSpeechProvider.modelName(), lipSyncVideoProvider.providerName(),
				lipSyncVideoProvider.modelName());
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	private static String trimAndCollapse(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ");
	}

	private record NormalizedInput(String word, String phrase, String normalizedWord, String normalizedPhrase,
			String language) {
	}

}
