package com.vocavista.backend.wordinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocavista.backend.api.model.WordInfoResponse;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class WordInfoService {

	private static final int MAX_WORD_LENGTH = 80;

	private final AiWordInfoProvider aiWordInfoProvider;
	private final ProviderWordInfoValidator providerWordInfoValidator;
	private final WordInfoMapper wordInfoMapper;
	private final WordInfoRepository wordInfoRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock = Clock.systemUTC();

	WordInfoResponse getWordInfo(String word) {
		String trimmedWord = trimAndValidate(word);
		String normalizedQuery = normalizeQuery(trimmedWord);
		return wordInfoRepository.findByNormalizedQuery(normalizedQuery)
				.map(this::toResponse)
				.orElseGet(() -> generateAndStore(trimmedWord, normalizedQuery));
	}

	private WordInfoResponse generateAndStore(String word, String normalizedQuery) {
		AiWordInfoResult providerResult = aiWordInfoProvider.generate(word);
		ProviderWordInfo providerWordInfo = keepFirstThreeExamples(providerResult.wordInfo());
		try {
			providerWordInfoValidator.validate(providerWordInfo);
		}
		catch (AiProviderBadGatewayException ex) {
			throw new AiProviderBadGatewayException(withRawResponse(ex.getMessage(), providerResult.rawResponse()), ex,
					providerResult.rawResponse());
		}
		WordInfoResponse response = wordInfoMapper.toApiResponse(providerWordInfo);
		return store(normalizedQuery, response);
	}

	private static ProviderWordInfo keepFirstThreeExamples(ProviderWordInfo wordInfo) {
		if (wordInfo == null || wordInfo.examples() == null || wordInfo.examples().size() <= 3) {
			return wordInfo;
		}
		return new ProviderWordInfo(wordInfo.normalizedWord(), wordInfo.language(), wordInfo.translations(),
				wordInfo.partOfSpeech(), wordInfo.gender(), wordInfo.article(), wordInfo.plural(), wordInfo.frequency(),
				wordInfo.isCompound(), wordInfo.compoundParts(), wordInfo.shortNote(),
				List.copyOf(wordInfo.examples().subList(0, 3)));
	}

	private WordInfoResponse store(String normalizedQuery, WordInfoResponse response) {
		UUID id = UUID.randomUUID();
		response.setId(id);
		try {
			OffsetDateTime now = OffsetDateTime.now(clock);
			wordInfoRepository.save(WordInfoRecord.create(id, normalizedQuery, response.getNormalizedWord(),
					response.getLanguage().getValue(), objectMapper.writeValueAsString(response), now));
			return response;
		}
		catch (DataIntegrityViolationException ex) {
			return wordInfoRepository.findByNormalizedQuery(normalizedQuery)
					.map(this::toResponse)
					.orElseThrow(() -> ex);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Could not store word info response", ex);
		}
	}

	private WordInfoResponse toResponse(WordInfoRecord record) {
		try {
			WordInfoResponse response = objectMapper.readValue(record.getResponseJson(), WordInfoResponse.class);
			response.setId(record.getId());
			return response;
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Could not read stored word info response", ex);
		}
	}

	private static String trimAndValidate(String word) {
		String trimmedWord = word == null ? "" : word.trim().replaceAll("\\s+", " ");
		if (!StringUtils.hasText(trimmedWord)) {
			throw new WordInfoValidationException("word must not be blank");
		}
		if (trimmedWord.length() > MAX_WORD_LENGTH) {
			throw new WordInfoValidationException("word must not exceed 80 characters");
		}
		return trimmedWord;
	}

	static String normalizeQuery(String word) {
		return word.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static String withRawResponse(String message, String rawResponse) {
		return rawResponse == null ? message : message + "; rawProviderResponse=" + rawResponse;
	}

}
