package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PhraseImageRequest;
import com.vocavista.backend.api.model.PhraseImageResponse;
import com.vocavista.backend.api.model.PhraseImageStatus;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class PhraseImageService {

	private static final int MAX_WORD_LENGTH = 80;
	private static final int MAX_PHRASE_LENGTH = 240;
	private static final String SUPPORTED_LANGUAGE = "de";

	private final PhraseImageRepository phraseImageRepository;
	private final PhraseImageGenerationProcessor generationProcessor;
	private final PhraseImageGenerator phraseImageGenerator;
	private final MediaStorageService mediaStorageService;
	private final WordInfoRepository wordInfoRepository;
	private final UserDictionaryService userDictionaryService;
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.image-prompt-version:v4}")
	private String promptVersion = "v4";

	@Transactional
	PhraseImageResponse create(PhraseImageRequest request) {
		NormalizedInput input = normalize(request);
		userDictionaryService.ensureEntryForCurrentUser(input.wordInfoRecord());
		String contentHash = contentHash(input);

		return phraseImageRepository
				.findByWordInfoRecordIdAndNormalizedPhrase(input.wordInfoRecord().getId(), input.normalizedPhrase())
				.map(asset -> reuseOrRetry(asset, contentHash))
				.orElseGet(() -> createQueuedAsset(input, contentHash));
	}

	@Transactional(readOnly = true)
	PhraseImageResponse get(UUID id) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		return toResponse(asset);
	}

	@Transactional(readOnly = true)
	StoredMedia getImage(UUID id) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		if (asset.getStatus() != PhraseImageAssetStatus.COMPLETED || !StringUtils.hasText(asset.getImageObjectKey())) {
			throw new PronunciationNotFoundException("Phrase image was not found");
		}
		return mediaStorageService.read(asset.getImageObjectKey());
	}

	@Transactional
	PhraseImageResponse regenerate(UUID id) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		userDictionaryService.ensureEntryForCurrentUser(asset.getWordInfoRecord());
		NormalizedInput input = new NormalizedInput(asset.getWordInfoRecord(), asset.getInputWord(), asset.getInputPhrase(),
				asset.getNormalizedWord(), asset.getNormalizedPhrase(), asset.getLanguage());
		return requeue(asset, contentHash(input));
	}

	private PhraseImageResponse createQueuedAsset(NormalizedInput input, String contentHash) {
		PhraseImageAsset asset = PhraseImageAsset.queued(input.wordInfoRecord(), input.word(), input.phrase(),
				input.normalizedWord(), input.normalizedPhrase(), input.language(), promptVersion, contentHash,
				OffsetDateTime.now(clock));
		try {
			PhraseImageAsset savedAsset = phraseImageRepository.save(asset);
			queueGeneration(savedAsset.getId());
			return toResponse(savedAsset);
		}
		catch (DataIntegrityViolationException ex) {
			return phraseImageRepository
					.findByWordInfoRecordIdAndNormalizedPhrase(input.wordInfoRecord().getId(), input.normalizedPhrase())
					.map(existingAsset -> reuseOrRetry(existingAsset, contentHash))
					.orElseThrow(() -> ex);
		}
	}

	private PhraseImageResponse reuseOrRetry(PhraseImageAsset asset, String contentHash) {
		if (asset.getStatus() != PhraseImageAssetStatus.FAILED) {
			return toResponse(asset);
		}
		return requeue(asset, contentHash);
	}

	private PhraseImageResponse requeue(PhraseImageAsset asset, String contentHash) {
		asset.setStatus(PhraseImageAssetStatus.QUEUED);
		asset.setPromptVersion(promptVersion);
		asset.setContentHash(contentHash);
		asset.setErrorCode(null);
		asset.setErrorMessage(null);
		asset.setCompletedAt(null);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		PhraseImageAsset savedAsset = phraseImageRepository.save(asset);
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

	PhraseImageResponse toResponse(PhraseImageAsset asset) {
		PhraseImageResponse response = new PhraseImageResponse(asset.getId(), asset.getWordInfoRecord().getId(),
				PhraseImageStatus.fromValue(asset.getStatus().name().toLowerCase()));
		response.setWord(asset.getNormalizedWord());
		response.setPhrase(asset.getNormalizedPhrase());
		if (StringUtils.hasText(asset.getImageObjectKey())) {
			response.setImageUrl(imageUri(asset));
		}
		if (asset.getStatus() == PhraseImageAssetStatus.FAILED) {
			response.setErrorCode(asset.getErrorCode());
			response.setErrorMessage(asset.getErrorMessage());
		}
		return response;
	}

	private NormalizedInput normalize(PhraseImageRequest request) {
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
		String value = String.join("\n", input.language(), input.normalizedWord().toLowerCase(),
				input.normalizedPhrase().toLowerCase(), promptVersion, phraseImageGenerator.providerName(),
				phraseImageGenerator.modelName());
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

	static URI imageUri(PhraseImageAsset asset) {
		return URI.create("/api/v1/media/phrase-images/" + asset.getId() + "/image");
	}

	private record NormalizedInput(WordInfoRecord wordInfoRecord, String word, String phrase, String normalizedWord,
			String normalizedPhrase, String language) {
	}

}
