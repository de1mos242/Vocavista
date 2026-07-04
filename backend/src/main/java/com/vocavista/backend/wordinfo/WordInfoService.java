package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.SaveVocabularyItemRequest;
import com.vocavista.backend.api.model.SaveVocabularyItemResponse;
import com.vocavista.backend.api.model.VocabularyItemDto;
import com.vocavista.backend.api.model.WordMeaningOption;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemMapper;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import com.vocavista.backend.vocabulary.VocabularyText;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class WordInfoService {

	private static final int MAX_WORD_LENGTH = 80;

	private final AiWordInfoProvider aiWordInfoProvider;
	private final ProviderWordInfoValidator providerWordInfoValidator;
	private final WordInfoMapper wordInfoMapper;
	private final VocabularyItemMapper vocabularyItemMapper;
	private final VocabularyItemRepository vocabularyItemRepository;
	private final Clock clock = Clock.systemUTC();

	@Transactional(readOnly = true)
	WordInfoResponse getWordInfo(String word) {
		String trimmedWord = trimAndValidate(word);
		AiWordInfoResult providerResult = aiWordInfoProvider.generate(trimmedWord);
		ProviderWordInfo providerWordInfo = normalizeProviderWordInfo(providerResult.wordInfo());
		try {
			providerWordInfoValidator.validate(providerWordInfo);
		}
		catch (AiProviderBadGatewayException ex) {
			throw new AiProviderBadGatewayException(withRawResponse(ex.getMessage(), providerResult.rawResponse()), ex,
					providerResult.rawResponse());
		}

		List<WordMeaningOption> meanings = assignOptionIds(wordInfoMapper.toMeaningOptions(providerWordInfo));
		return new WordInfoResponse(trimmedWord, wordInfoMapper.toInputLanguage(providerWordInfo), meanings);
	}

	@Transactional
	SaveVocabularyItemResponse saveVocabularyItem(SaveVocabularyItemRequest request) {
		if (request == null || request.getItem() == null) {
			throw new WordInfoValidationException("item is required");
		}
		VocabularyItemDto item = request.getItem();
		String language = validateVocabularyField(VocabularyText.languageCode(item.getLanguage()), "language");
		String word = validateVocabularyField(VocabularyText.optional(item.getWord()), "word");
		String phrase = validateVocabularyField(VocabularyText.optional(item.getPhrase()), "phrase");
		Optional<VocabularyItem> existingItem = vocabularyItemRepository
				.findFirstByLanguageAndWordIgnoreCaseAndPhraseIgnoreCase(language, word, phrase);
		if (existingItem.isPresent()) {
			return new SaveVocabularyItemResponse(wordInfoMapper.toApiItem(existingItem.get()));
		}

		OffsetDateTime now = OffsetDateTime.now(clock);
		VocabularyItem saved = vocabularyItemMapper.toEntity(item, UUID.randomUUID(), now);

		try {
			return new SaveVocabularyItemResponse(wordInfoMapper.toApiItem(vocabularyItemRepository.save(saved)));
		}
		catch (DataIntegrityViolationException ex) {
			return vocabularyItemRepository
					.findFirstByLanguageAndWordIgnoreCaseAndPhraseIgnoreCase(language, word, phrase)
					.map(wordInfoMapper::toApiItem)
					.map(SaveVocabularyItemResponse::new)
					.orElseThrow(() -> ex);
		}
	}

	private static String validateVocabularyField(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			throw new WordInfoValidationException(fieldName + " must not be blank");
		}
		return value;
	}

	private static ProviderWordInfo normalizeProviderWordInfo(ProviderWordInfo wordInfo) {
		if (wordInfo == null || wordInfo.meanings() == null) {
			return wordInfo;
		}
		return new ProviderWordInfo(wordInfo.inputLanguage(), wordInfo.meanings().stream()
				.map(WordInfoService::normalizeMeaning)
				.toList());
	}

	private static ProviderWordInfo.WordMeaning normalizeMeaning(ProviderWordInfo.WordMeaning meaning) {
		if (meaning == null || meaning.partOfSpeech() != ProviderWordInfo.ProviderPartOfSpeech.noun
				|| meaning.gender() == null || meaning.gender().isEmpty()) {
			return meaning;
		}
		ProviderWordInfo.ProviderArticle article = articleFor(meaning.gender().get());
		if (meaning.article() != null && meaning.article().filter(article::equals).isPresent()) {
			return meaning;
		}
		return new ProviderWordInfo.WordMeaning(meaning.normalizedWord(), meaning.language(), meaning.translations(),
				meaning.partOfSpeech(), meaning.gender(), Optional.of(article), meaning.plural(),
				meaning.frequency(), meaning.isCompound(), meaning.compoundParts(), meaning.shortNote(), meaning.examples());
	}

	private static List<WordMeaningOption> assignOptionIds(List<WordMeaningOption> meanings) {
		for (int index = 0; index < meanings.size(); index++) {
			meanings.get(index).optionId(index);
		}
		return meanings;
	}

	private static ProviderWordInfo.ProviderArticle articleFor(ProviderWordInfo.ProviderGender gender) {
		return switch (gender) {
			case masculine -> ProviderWordInfo.ProviderArticle.der;
			case feminine -> ProviderWordInfo.ProviderArticle.die;
			case neuter -> ProviderWordInfo.ProviderArticle.das;
		};
	}

	private static String trimAndValidate(String word) {
		String trimmedWord = trimAndCollapse(word);
		if (!StringUtils.hasText(trimmedWord)) {
			throw new WordInfoValidationException("word must not be blank");
		}
		if (trimmedWord.length() > MAX_WORD_LENGTH) {
			throw new WordInfoValidationException("word must not exceed 80 characters");
		}
		return trimmedWord;
	}

	private static String trimAndCollapse(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ");
	}

	private static String withRawResponse(String message, String rawResponse) {
		return rawResponse == null ? message : message + "; rawProviderResponse=" + rawResponse;
	}

}
