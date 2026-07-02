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
		if (wordInfo.inputLanguage() == null) {
			throw malformed("inputLanguage is required");
		}
		if (wordInfo.meanings() == null || wordInfo.meanings().isEmpty()) {
			throw malformed("meanings must contain at least 1 item");
		}
		if (wordInfo.meanings().size() > 5) {
			throw malformed("meanings must not contain more than 5 items");
		}
		wordInfo.meanings().forEach(this::validateMeaning);
	}

	private void validateMeaning(ProviderWordInfo.WordMeaning meaning) {
		if (meaning == null) {
			throw malformed("meanings must not contain null items");
		}
		requireText(meaning.normalizedWord(), "meaning.normalizedWord");
		if (meaning.language() == null) {
			throw malformed("language is required");
		}
		if (meaning.language() != ProviderWordInfo.Language.de) {
			throw malformed("language must be de");
		}
		requireLocalizedText(meaning.translations(), "meaning.translations");
		requireValue(meaning.partOfSpeech(), "meaning.partOfSpeech");
		requireValue(meaning.frequency(), "meaning.frequency");
		if (meaning.isCompound() == null) {
			throw malformed("isCompound is required");
		}
		if (meaning.compoundParts() == null) {
			throw malformed("compoundParts is required");
		}
		if (!meaning.isCompound() && !meaning.compoundParts().isEmpty()) {
			throw malformed("compoundParts must be empty for non-compound words");
		}
		meaning.compoundParts().forEach(this::validateCompoundPart);
		requireLocalizedText(meaning.shortNote(), "meaning.shortNote");
		if (meaning.examples() == null || meaning.examples().size() < 3) {
			throw malformed("examples must contain at least 3 items");
		}
		meaning.examples().forEach(this::validateExample);
		if (meaning.partOfSpeech() == ProviderWordInfo.ProviderPartOfSpeech.noun) {
			requireOptionalValue(meaning.gender(), "meaning.gender");
			requireOptionalValue(meaning.article(), "meaning.article");
			requireOptionalText(meaning.plural(), "meaning.plural");
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
