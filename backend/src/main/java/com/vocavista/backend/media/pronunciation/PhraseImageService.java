package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.PhraseImageRequest;
import com.vocavista.backend.api.model.PhraseImageResponse;
import com.vocavista.backend.dictionary.UserDictionaryService;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
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
	private final MediaStorageService mediaStorageService;
	private final VocabularyItemRepository vocabularyItemRepository;
	private final UserDictionaryService userDictionaryService;
	private final MediaResponseMapper mediaResponseMapper;
	private final Clock clock = Clock.systemUTC();

	@Value("${vocavista.media.image-prompt-version:v4}")
	private String promptVersion = "v4";

	@Transactional
	PhraseImageResponse create(PhraseImageRequest request) {
		NormalizedInput input = normalize(request);
		userDictionaryService.ensureEntryForCurrentUser(input.vocabularyItem());

		return phraseImageRepository
				.findByVocabularyItemIdAndPhraseIgnoreCase(input.vocabularyItem().getId(), input.phrase())
				.map(this::reuseOrRetry)
				.orElseGet(() -> createQueuedAsset(input));
	}

	@Transactional(readOnly = true)
	PhraseImageResponse get(UUID id) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		return mediaResponseMapper.toResponse(asset);
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

	@Transactional(readOnly = true)
	StoredMedia getCandidateImage(UUID id, int candidateIndex) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		validateCandidateIndex(asset, candidateIndex);
		return mediaStorageService.read(candidateObjectKey(asset.getId(), candidateIndex));
	}

	@Transactional
	PhraseImageResponse selectCandidate(UUID id, int candidateIndex) {
		PhraseImageAsset asset = phraseImageRepository.findById(id)
				.orElseThrow(() -> new PronunciationNotFoundException("Phrase image asset was not found"));
		if (asset.getStatus() == PhraseImageAssetStatus.COMPLETED) {
			return mediaResponseMapper.toResponse(asset);
		}
		if (asset.getStatus() != PhraseImageAssetStatus.AWAITING_SELECTION) {
			throw new PronunciationNotFoundException("Phrase image candidate was not found");
		}
		validateCandidateIndex(asset, candidateIndex);
		asset.setImageObjectKey(candidateObjectKey(asset.getId(), candidateIndex));
		asset.setStatus(PhraseImageAssetStatus.COMPLETED);
		asset.setErrorCode(null);
		asset.setErrorMessage(null);
		asset.setCompletedAt(OffsetDateTime.now(clock));
		asset.setUpdatedAt(asset.getCompletedAt());
		return mediaResponseMapper.toResponse(phraseImageRepository.save(asset));
	}

	private PhraseImageResponse createQueuedAsset(NormalizedInput input) {
		PhraseImageAsset asset = PhraseImageAsset.queued(input.vocabularyItem(), input.word(), input.phrase(), input.language(),
				promptVersion, OffsetDateTime.now(clock));
		try {
			PhraseImageAsset savedAsset = phraseImageRepository.save(asset);
			queueGeneration(savedAsset.getId());
			return mediaResponseMapper.toResponse(savedAsset);
		}
		catch (DataIntegrityViolationException ex) {
			return phraseImageRepository
					.findByVocabularyItemIdAndPhraseIgnoreCase(input.vocabularyItem().getId(), input.phrase())
					.map(this::reuseOrRetry)
					.orElseThrow(() -> ex);
		}
	}

	private PhraseImageResponse reuseOrRetry(PhraseImageAsset asset) {
		if (asset.getStatus() != PhraseImageAssetStatus.FAILED) {
			return mediaResponseMapper.toResponse(asset);
		}
		return requeue(asset);
	}

	private PhraseImageResponse requeue(PhraseImageAsset asset) {
		asset.setStatus(PhraseImageAssetStatus.QUEUED);
		asset.setPromptVersion(promptVersion);
		asset.setErrorCode(null);
		asset.setErrorMessage(null);
		asset.setCompletedAt(null);
		asset.setImageObjectKey(null);
		asset.setImageCandidateCount(0);
		asset.setUpdatedAt(OffsetDateTime.now(clock));
		PhraseImageAsset savedAsset = phraseImageRepository.save(asset);
		queueGeneration(savedAsset.getId());
		return mediaResponseMapper.toResponse(savedAsset);
	}

	static String candidateObjectKey(UUID id, int candidateIndex) {
		return "phrase-images/" + id + "/candidates/" + candidateIndex + "/image";
	}

	private static void validateCandidateIndex(PhraseImageAsset asset, int candidateIndex) {
		if (candidateIndex < 0 || candidateIndex >= asset.getImageCandidateCount()) {
			throw new PronunciationNotFoundException("Phrase image candidate was not found");
		}
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
		VocabularyItem vocabularyItem = vocabularyItemRepository.findById(wordInfoId)
				.orElseThrow(() -> new PronunciationValidationException("wordInfoId must reference an existing vocabulary item"));
		return new NormalizedInput(vocabularyItem, word, phrase, language);
	}

	private static String trimAndCollapse(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ");
	}

	private record NormalizedInput(VocabularyItem vocabularyItem, String word, String phrase, String language) {
	}

}
