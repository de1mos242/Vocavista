package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.WordInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
class WordInfoService {

	private static final int MAX_WORD_LENGTH = 80;

	private final AiWordInfoProvider aiWordInfoProvider;
	private final ProviderWordInfoValidator providerWordInfoValidator;
	private final WordInfoMapper wordInfoMapper;

	WordInfoResponse getWordInfo(String word) {
		String trimmedWord = trimAndValidate(word);
		ProviderWordInfo providerWordInfo = aiWordInfoProvider.generate(trimmedWord);
		providerWordInfoValidator.validate(providerWordInfo);
		return wordInfoMapper.toApiResponse(providerWordInfo);
	}

	private static String trimAndValidate(String word) {
		String trimmedWord = word == null ? "" : word.trim();
		if (!StringUtils.hasText(trimmedWord)) {
			throw new WordInfoValidationException("word must not be blank");
		}
		if (trimmedWord.length() > MAX_WORD_LENGTH) {
			throw new WordInfoValidationException("word must not exceed 80 characters");
		}
		return trimmedWord;
	}

}
