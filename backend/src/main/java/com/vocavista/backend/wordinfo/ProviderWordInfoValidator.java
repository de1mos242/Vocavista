package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.Gender;
import com.vocavista.backend.api.model.GermanArticle;
import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordFrequency;
import com.vocavista.backend.api.model.WordInfoResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ProviderWordInfoValidator {

	void validate(ProviderWordInfo wordInfo) {
		if (wordInfo == null) {
			throw malformed("missing word info");
		}
		requireText(wordInfo.normalizedWord(), "normalizedWord");
		requireEnum(wordInfo.language(), "language", WordInfoResponse.LanguageEnum::fromValue);
		if (!"de".equals(wordInfo.language())) {
			throw malformed("language must be de");
		}
		requireLanguageMap(wordInfo.translations(), "translations");
		requireEnum(wordInfo.partOfSpeech(), "partOfSpeech", PartOfSpeech::fromValue);
		requireEnum(wordInfo.frequency(), "frequency", WordFrequency::fromValue);
		if (wordInfo.isCompound() == null) {
			throw malformed("isCompound is required");
		}
		if (wordInfo.compoundParts() == null) {
			throw malformed("compoundParts is required");
		}
		if (!wordInfo.isCompound() && !wordInfo.compoundParts().isEmpty()) {
			throw malformed("compoundParts must be empty for non-compound words");
		}
		wordInfo.compoundParts().forEach(this::validateCompoundPart);
		requireLanguageMap(wordInfo.shortNote(), "shortNote");
		if (wordInfo.examples() == null || wordInfo.examples().size() != 3) {
			throw malformed("examples must contain exactly 3 items");
		}
		wordInfo.examples().forEach(this::validateExample);
		if ("noun".equals(wordInfo.partOfSpeech())) {
			requireEnum(wordInfo.gender(), "gender", Gender::fromValue);
			requireEnum(wordInfo.article(), "article", GermanArticle::fromValue);
			requireText(wordInfo.plural(), "plural");
		}
	}

	private void validateCompoundPart(ProviderWordInfo.CompoundPart compoundPart) {
		if (compoundPart == null) {
			throw malformed("compoundParts must not contain null items");
		}
		requireText(compoundPart.word(), "compoundPart.word");
		requireLanguageMap(compoundPart.meanings(), "compoundPart.meanings");
	}

	private void validateExample(ProviderWordInfo.WordExample example) {
		if (example == null) {
			throw malformed("examples must not contain null items");
		}
		requireText(example.sentence(), "example.sentence");
		requireLanguageMap(example.translations(), "example.translations");
	}

	private static void requireLanguageMap(Map<String, List<String>> textMap, String fieldName) {
		if (textMap == null || textMap.isEmpty()) {
			throw malformed(fieldName + " is required");
		}
		requireLanguageValues(textMap, fieldName, "en");
		requireLanguageValues(textMap, fieldName, "ru");
	}

	private static void requireLanguageValues(Map<String, List<String>> textMap, String fieldName, String language) {
		List<String> values = textMap.get(language);
		if (values == null || values.isEmpty()) {
			throw malformed(fieldName + " must include " + language);
		}
		for (String value : values) {
			requireText(value, fieldName + "." + language);
		}
	}

	private static void requireText(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			throw malformed(fieldName + " is required");
		}
	}

	private static <T> void requireEnum(String value, String fieldName, EnumParser<T> parser) {
		requireText(value, fieldName);
		try {
			parser.parse(value.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new AiProviderBadGatewayException("AI provider returned unsupported enum value for " + fieldName, ex);
		}
	}

	private static AiProviderBadGatewayException malformed(String message) {
		return new AiProviderBadGatewayException("AI provider returned malformed content: " + message);
	}

	@FunctionalInterface
	private interface EnumParser<T> {

		T parse(String value);

	}

}
