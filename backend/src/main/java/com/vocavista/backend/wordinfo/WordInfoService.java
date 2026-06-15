package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.SaveVocabularyItemRequest;
import com.vocavista.backend.api.model.SaveVocabularyItemResponse;
import com.vocavista.backend.api.model.VocabularyItemDto;
import com.vocavista.backend.api.model.WordInfoResponse;
import com.vocavista.backend.vocabulary.VocabularyItem;
import com.vocavista.backend.vocabulary.VocabularyItemMapper;
import com.vocavista.backend.vocabulary.VocabularyItemRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

		VocabularyItemDto proposedItem = wordInfoMapper.toProposedItem(keepFirstExample(providerWordInfo));
		List<VocabularyItemDto> existingItems = vocabularyItemRepository
				.findByLanguageAndWordIgnoreCase(proposedItem.getLanguage(), proposedItem.getWord())
				.stream()
				.map(wordInfoMapper::toApiItem)
				.toList();
		return new WordInfoResponse(trimmedWord, proposedItem.getWord(), existingItems, proposedItem);
	}

	@Transactional
	SaveVocabularyItemResponse saveVocabularyItem(SaveVocabularyItemRequest request) {
		if (request == null || request.getItem() == null) {
			throw new WordInfoValidationException("item is required");
		}
		VocabularyItemDto item = request.getItem();

		OffsetDateTime now = OffsetDateTime.now(clock);
		VocabularyItem saved = vocabularyItemMapper.toEntity(item, UUID.randomUUID(), now);

		return new SaveVocabularyItemResponse(wordInfoMapper.toApiItem(vocabularyItemRepository.save(saved)));
	}

	private static ProviderWordInfo keepFirstExample(ProviderWordInfo wordInfo) {
		if (wordInfo == null || wordInfo.examples() == null || wordInfo.examples().size() <= 1) {
			return wordInfo;
		}
		return new ProviderWordInfo(wordInfo.normalizedWord(), wordInfo.language(), wordInfo.translations(),
				wordInfo.partOfSpeech(), wordInfo.gender(), wordInfo.article(), wordInfo.plural(), wordInfo.frequency(),
				wordInfo.isCompound(), wordInfo.compoundParts(), wordInfo.shortNote(),
				List.copyOf(wordInfo.examples().subList(0, 1)));
	}

	private static ProviderWordInfo normalizeProviderWordInfo(ProviderWordInfo wordInfo) {
		if (wordInfo == null
				|| wordInfo.partOfSpeech() != ProviderWordInfo.ProviderPartOfSpeech.noun
				|| wordInfo.article() == null
				|| wordInfo.article().isPresent()
				|| wordInfo.gender() == null
				|| wordInfo.gender().isEmpty()) {
			return wordInfo;
		}
		return new ProviderWordInfo(wordInfo.normalizedWord(), wordInfo.language(), wordInfo.translations(),
				wordInfo.partOfSpeech(), wordInfo.gender(), Optional.of(articleFor(wordInfo.gender().get())), wordInfo.plural(),
				wordInfo.frequency(), wordInfo.isCompound(), wordInfo.compoundParts(), wordInfo.shortNote(), wordInfo.examples());
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
