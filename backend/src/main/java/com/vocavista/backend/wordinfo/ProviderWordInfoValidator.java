package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ProviderWordInfoValidator {

	void validate(ProviderWordInfo wordInfo) {
		if (wordInfo == null) {
			throw malformed("missing word info");
		}
		requireText(wordInfo.normalizedWord(), "normalizedWord");
		if (wordInfo.language() == null) {
			throw malformed("language is required");
		}
		if (wordInfo.language() != ProviderWordInfo.Language.de) {
			throw malformed("language must be de");
		}
		requireLocalizedText(wordInfo.translations(), "translations");
		requireValue(wordInfo.partOfSpeech(), "partOfSpeech");
		requireValue(wordInfo.frequency(), "frequency");
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
		requireLocalizedText(wordInfo.shortNote(), "shortNote");
		if (wordInfo.examples() == null || wordInfo.examples().size() < 3) {
			throw malformed("examples must contain at least 3 items");
		}
		wordInfo.examples().forEach(this::validateExample);
		if (wordInfo.partOfSpeech() == ProviderWordInfo.ProviderPartOfSpeech.noun) {
			requireOptionalValue(wordInfo.gender(), "gender");
			requireOptionalValue(wordInfo.article(), "article");
			requireOptionalText(wordInfo.plural(), "plural");
		}
	}

	private void validateCompoundPart(ProviderWordInfo.CompoundPart compoundPart) {
		if (compoundPart == null) {
			throw malformed("compoundParts must not contain null items");
		}
		requireText(compoundPart.word(), "compoundPart.word");
		requireLocalizedText(compoundPart.meanings(), "compoundPart.meanings");
	}

	private void validateExample(ProviderWordInfo.WordExample example) {
		if (example == null) {
			throw malformed("examples must not contain null items");
		}
		requireText(example.sentence(), "example.sentence");
		requireLocalizedText(example.translations(), "example.translations");
	}

	private static void requireLocalizedText(ProviderWordInfo.LocalizedText text, String fieldName) {
		if (text == null) {
			throw malformed(fieldName + " is required");
		}
		requireLanguageValues(text.en(), fieldName, "en");
		requireLanguageValues(text.ru(), fieldName, "ru");
	}

	private static void requireLanguageValues(List<String> values, String fieldName, String language) {
		if (values == null || values.isEmpty()) {
			throw malformed(fieldName + " must include " + language);
		}
		for (String value : values) {
			requireText(value, fieldName + "." + language);
		}
	}

	private static void requireValue(Object value, String fieldName) {
		if (value == null) {
			throw malformed(fieldName + " is required");
		}
	}

	private static void requireOptionalValue(Optional<?> value, String fieldName) {
		if (value == null || value.isEmpty()) {
			throw malformed(fieldName + " is required");
		}
	}

	private static void requireOptionalText(Optional<String> value, String fieldName) {
		if (value == null || value.isEmpty()) {
			throw malformed(fieldName + " is required");
		}
		requireText(value.get(), fieldName);
	}

	private static void requireText(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			throw malformed(fieldName + " is required");
		}
	}

	private static AiProviderBadGatewayException malformed(String message) {
		return new AiProviderBadGatewayException("AI provider returned malformed content: " + message);
	}

}
