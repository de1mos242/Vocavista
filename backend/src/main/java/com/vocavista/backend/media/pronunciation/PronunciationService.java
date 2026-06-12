package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PronunciationRequest;
import com.vocavista.backend.api.model.PronunciationResponse;
import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.dictionary.UserDictionaryService;
import com.vocavista.backend.wordinfo.WordInfoRecord;
import com.vocavista.backend.wordinfo.WordInfoRepository;
import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class PronunciationService {

	private static final int MAX_WORD_LENGTH = 80;
	private static final int MAX_PHRASE_LENGTH = 240;
	private static final String SUPPORTED_LANGUAGE = "de";

	private final PronunciationRepository pronunciationRepository;
	private final PronunciationGenerationProcessor generationProcessor;
	private final PronunciationVideoCompressor pronunciationVideoCompressor;
	private final MediaStorageService mediaStorageService;
	private final WordInfoRepository wordInfoRepository;
	private final UserDictionaryService userDictionaryService;
	private final Clock clock = Clock.systemUTC();

	@Transactional
	PronunciationResponse create(PronunciationRequest request) {
		NormalizedInput input = normalize(request);
		userDictionaryService.ensureEntryForCurrentUser(input.wordInfoRecord());

		return pronunciationRepository
				.findByWordInfoRecordIdAndNormalizedPhrase(input.wordInfoRecord().getId(), input.normalizedPhrase())
				.map(this::reuseOrRetry)
				.orElseGet(() -> createQueuedAsset(input));
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

	@Transactional
	PronunciationResponse regenerate(UUID id) {
		PronunciationAsset asset = pronunciationRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Pronunciation asset was not found"));
		userDictionaryService.ensureEntryForCurrentUser(asset.getWordInfoRecord());
		return requeue(asset);
	}

	private StoredMedia storeSmallVideo(PronunciationAsset asset, GeneratedVideo smallVideo) {
		String smallVideoObjectKey = "pronunciations/" + asset.getId() + "/video-small.mp4";
		mediaStorageService.store(smallVideoObjectKey, smallVideo.contentType(), smallVideo.bytes());
		asset.setSmallVideoObjectKey(smallVideoObjectKey);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		pronunciationRepository.save(asset);
		return new StoredMedia(smallVideo.contentType(), smallVideo.bytes());
	}

	private PronunciationResponse createQueuedAsset(NormalizedInput input) {
		PronunciationAsset asset = PronunciationAsset.queued(input.wordInfoRecord(), input.word(), input.phrase(), input.normalizedWord(),
				input.normalizedPhrase(), input.language(), OffsetDateTime.now(clock));
		try {
			PronunciationAsset savedAsset = pronunciationRepository.save(asset);
			queueGeneration(savedAsset.getId());
			return toResponse(savedAsset);
		}
		catch (DataIntegrityViolationException ex) {
			return pronunciationRepository
					.findByWordInfoRecordIdAndNormalizedPhrase(input.wordInfoRecord().getId(), input.normalizedPhrase())
					.map(this::reuseOrRetry)
					.orElseThrow(() -> ex);
		}
	}

	private PronunciationResponse reuseOrRetry(PronunciationAsset asset) {
		if (asset.getStatus() != PronunciationAssetStatus.FAILED) {
			return toResponse(asset);
		}
		return requeue(asset);
	}

	private PronunciationResponse requeue(PronunciationAsset asset) {
		asset.setStatus(PronunciationAssetStatus.QUEUED);
		asset.setErrorCode(null);
		asset.setErrorMessage(null);
		asset.setCompletedAt(null);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		PronunciationAsset savedAsset = pronunciationRepository.save(asset);
		queueGeneration(savedAsset.getId());
		return toResponse(savedAsset);
	}

	private void queueGeneration(UUID id) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			generationProcessor.process(id);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				generationProcessor.process(id);
			}
		});
	}

	private PronunciationResponse toResponse(PronunciationAsset asset) {
		PronunciationResponse response = new PronunciationResponse(asset.getId(),
				asset.getWordInfoRecord().getId(), PronunciationStatus.fromValue(asset.getStatus().name().toLowerCase()));
		if (StringUtils.hasText(asset.getVideoObjectKey())) {
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
