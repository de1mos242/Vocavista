package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PronunciationRequest;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.dictionary.UserDictionaryService;
import com.vocavista.backend.wordinfo.WordInfoRecord;
import com.vocavista.backend.wordinfo.WordInfoRepository;
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
class PronunciationService {

	private static final int MAX_WORD_LENGTH = 80;
	private static final int MAX_PHRASE_LENGTH = 240;
	private static final String SUPPORTED_LANGUAGE = "de";

	private final PronunciationRepository pronunciationRepository;
	private final PronunciationGenerationProcessor generationProcessor;
	private final PronunciationVideoGenerator pronunciationVideoGenerator;
	private final PronunciationVideoCompressor pronunciationVideoCompressor;
	private final MediaStorageService mediaStorageService;
	private final WordInfoRepository wordInfoRepository;
	private final UserDictionaryService userDictionaryService;
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.script-template-version:v6}")
	private String scriptTemplateVersion = "v6";

	PronunciationResponse create(PronunciationRequest request) {
		NormalizedInput input = normalize(request);
		userDictionaryService.ensureEntryForCurrentUser(input.wordInfoRecord());
		String contentHash = contentHash(input);

		return pronunciationRepository
				.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(input.language(), contentHash)
				.map(this::reuseOrRetry)
				.orElseGet(() -> createQueuedAsset(input, contentHash));
	}

	PronunciationResponse get(UUID id) {
		PronunciationAsset asset = pronunciationRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Pronunciation asset was not found"));
		return toResponse(asset);
	}

	StoredMedia getVideo(UUID id) {
		PronunciationAsset asset = pronunciationRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Pronunciation asset was not found"));
		if (asset.getStatus() != PronunciationAssetStatus.COMPLETED || !StringUtils.hasText(asset.getVideoObjectKey())) {
			throw new PronunciationNotFoundException("Pronunciation video was not found");
		}
		return mediaStorageService.read(asset.getVideoObjectKey());
	}

	StoredMedia getSmallVideo(UUID id) {
		PronunciationAsset asset = pronunciationRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Pronunciation asset was not found"));
		if (asset.getStatus() != PronunciationAssetStatus.COMPLETED || !StringUtils.hasText(asset.getVideoObjectKey())) {
			throw new PronunciationNotFoundException("Pronunciation video was not found");
		}
		if (StringUtils.hasText(asset.getSmallVideoObjectKey())) {
			return mediaStorageService.read(asset.getSmallVideoObjectKey());
		}

		StoredMedia originalVideo = mediaStorageService.read(asset.getVideoObjectKey());
		return pronunciationVideoCompressor
				.compress(new GeneratedVideo(originalVideo.bytes(), originalVideo.contentType() == null ? "video/mp4" : originalVideo.contentType()))
				.map(smallVideo -> storeSmallVideo(asset, smallVideo))
				.orElse(originalVideo);
	}

	private StoredMedia storeSmallVideo(PronunciationAsset asset, GeneratedVideo smallVideo) {
		String smallVideoObjectKey = "pronunciations/" + asset.getId() + "/video-small.mp4";
		mediaStorageService.store(smallVideoObjectKey, smallVideo.contentType(), smallVideo.bytes());
		asset.setSmallVideoObjectKey(smallVideoObjectKey);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		pronunciationRepository.save(asset);
		return new StoredMedia(smallVideo.contentType(), smallVideo.bytes());
	}

	private PronunciationResponse createQueuedAsset(NormalizedInput input, String contentHash) {
		PronunciationAsset asset = PronunciationAsset.queued(input.wordInfoRecord(), input.word(), input.phrase(), input.normalizedWord(),
				input.normalizedPhrase(), input.language(), contentHash, OffsetDateTime.now(clock));
		try {
			PronunciationAsset savedAsset = pronunciationRepository.save(asset);
			generationProcessor.process(savedAsset.getId());
			return toResponse(savedAsset);
		}
		catch (DataIntegrityViolationException ex) {
			return pronunciationRepository
					.findFirstByLanguageAndContentHashOrderByCreatedAtAsc(input.language(), contentHash)
					.map(this::reuseOrRetry)
					.orElseThrow(() -> ex);
		}
	}

	private PronunciationResponse reuseOrRetry(PronunciationAsset asset) {
		if (asset.getStatus() != PronunciationAssetStatus.FAILED) {
			return toResponse(asset);
		}

		asset.setStatus(PronunciationAssetStatus.QUEUED);
		asset.setVideoObjectKey(null);
		asset.setSmallVideoObjectKey(null);
		asset.setVideoProvider(null);
		asset.setVideoModel(null);
		asset.setErrorCode(null);
		asset.setErrorMessage(null);
		asset.setCompletedAt(null);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		PronunciationAsset savedAsset = pronunciationRepository.save(asset);
		generationProcessor.process(savedAsset.getId());
		return toResponse(savedAsset);
	}

	private PronunciationResponse toResponse(PronunciationAsset asset) {
		PronunciationResponse response = new PronunciationResponse(asset.getId(),
				asset.getWordInfoRecord().getId(), PronunciationStatus.fromValue(asset.getStatus().name().toLowerCase()));
		if (asset.getStatus() == PronunciationAssetStatus.COMPLETED && StringUtils.hasText(asset.getVideoObjectKey())) {
			response.setVideoUrl(smallVideoUri(asset));
			response.setFullVideoUrl(fullVideoUri(asset));
		}
		if (asset.getStatus() == PronunciationAssetStatus.FAILED) {
			response.setErrorCode(asset.getErrorCode());
			response.setErrorMessage(asset.getErrorMessage());
		}
		return response;
	}

	private NormalizedInput normalize(PronunciationRequest request) {
		if (request == null) {
			throw new PronunciationValidationException("request body is required");
		}

		String word = trimAndCollapse(request.getWord());
		String phrase = trimAndCollapse(request.getPhrase());
		String language = request.getLanguage() == null ? "" : request.getLanguage().toString();
		UUID wordInfoId = request.getWordInfoId();
		if (wordInfoId == null) {
			throw new PronunciationValidationException("wordInfoId is required");
		}
		if (!StringUtils.hasText(word)) {
			throw new PronunciationValidationException("word must not be blank");
		}
		if (word.length() > MAX_WORD_LENGTH) {
			throw new PronunciationValidationException("word must not exceed 80 characters");
		}
		if (!StringUtils.hasText(phrase)) {
			throw new PronunciationValidationException("phrase must not be blank");
		}
		if (phrase.length() > MAX_PHRASE_LENGTH) {
			throw new PronunciationValidationException("phrase must not exceed 240 characters");
		}
		if (!SUPPORTED_LANGUAGE.equals(language)) {
			throw new PronunciationValidationException("only German language code de is supported");
		}
		WordInfoRecord wordInfoRecord = wordInfoRepository.findById(wordInfoId)
				.orElseThrow(() -> new PronunciationValidationException("wordInfoId must reference an existing word info record"));
		return new NormalizedInput(wordInfoRecord, request.getWord(), request.getPhrase(), word, phrase, language);
	}

	private String contentHash(NormalizedInput input) {
		String value = String.join("\n", input.language(), input.wordInfoRecord().getId().toString(), input.normalizedWord().toLowerCase(),
				input.normalizedPhrase().toLowerCase(), scriptTemplateVersion, pronunciationVideoGenerator.providerName(),
				pronunciationVideoGenerator.modelName());
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

	private record NormalizedInput(WordInfoRecord wordInfoRecord, String word, String phrase, String normalizedWord, String normalizedPhrase,
			String language) {
	}

	static URI smallVideoUri(PronunciationAsset asset) {
		return URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video/small");
	}

	static URI fullVideoUri(PronunciationAsset asset) {
		return URI.create("/api/v1/media/pronunciations/" + asset.getId() + "/video");
	}

}
