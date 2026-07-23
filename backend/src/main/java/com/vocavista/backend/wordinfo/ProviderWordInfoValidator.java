package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ProviderWordInfoValidator {

	void validate(ProviderWordInfo wordInfo) {
		validate(wordInfo, null);
	}

	void validate(ProviderWordInfo wordInfo, String inputWord) {
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
		wordInfo.meanings().forEach(meaning -> validateMeaning(meaning, wordInfo.inputLanguage(), inputWord));
		validateDistinctMeaningCandidates(wordInfo.meanings());
	}

	private void validateMeaning(ProviderWordInfo.WordMeaning meaning, ProviderWordInfo.InputLanguage inputLanguage,
			String inputWord) {
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
		requireLocalizedText(meaning.gloss(), "meaning.gloss");
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
		meaning.examples().forEach(example -> validateExample(example, inputLanguage, inputWord, meaning.normalizedWord()));
		if (meaning.partOfSpeech() == ProviderWordInfo.ProviderPartOfSpeech.noun) {
			requireOptionalValue(meaning.gender(), "meaning.gender");
			requireOptionalValue(meaning.article(), "meaning.article");
			requireOptionalText(meaning.plural(), "meaning.plural");
		}
	}

	private static void validateDistinctMeaningCandidates(List<ProviderWordInfo.WordMeaning> meanings) {
		Set<String> candidates = new HashSet<>();
		Set<String> glosses = new HashSet<>();
		for (ProviderWordInfo.WordMeaning meaning : meanings) {
			if (!candidates.add(normalizeText(meaning.normalizedWord()).toLowerCase())) {
				throw malformed("meanings must not contain duplicate normalized German candidates");
			}
			String normalizedGloss = normalizedLocalizedText(meaning.gloss());
			if (!glosses.add(normalizedGloss)) {
				throw malformed("meanings must not contain duplicate localized glosses");
			}
		}
	}

	private void validateCompoundPart(ProviderWordInfo.CompoundPart compoundPart) {
		if (compoundPart == null) {
			throw malformed("compoundParts must not contain null items");
		}
		requireText(compoundPart.word(), "compoundPart.word");
		requireLocalizedText(compoundPart.meanings(), "compoundPart.meanings");
	}

	private void validateExample(ProviderWordInfo.WordExample example, ProviderWordInfo.InputLanguage inputLanguage,
			String inputWord, String normalizedWord) {
		if (example == null) {
			throw malformed("examples must not contain null items");
		}
		requireText(example.sentence(), "example.sentence");
		if (!normalizeText(example.sentence()).toLowerCase().contains(normalizeText(normalizedWord).toLowerCase())) {
			throw malformed("German examples must use the declared German meaning candidate");
		}
		if (inputLanguage != ProviderWordInfo.InputLanguage.de
				&& !sameNormalizedText(inputWord, normalizedWord)
				&& containsWholeInput(example.sentence(), inputWord)) {
			throw malformed("German examples must not contain the source-language input word");
		}
		requireLocalizedText(example.translations(), "example.translations");
	}

	private static boolean sameNormalizedText(String left, String right) {
		return normalizeText(left).equalsIgnoreCase(normalizeText(right));
	}

	private static boolean containsWholeInput(String sentence, String inputWord) {
		String normalizedInput = normalizeText(inputWord);
		if (!StringUtils.hasText(normalizedInput)) {
			return false;
		}
		String pattern = "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedInput) + "(?![\\p{L}\\p{N}])";
		return Pattern.compile(pattern).matcher(normalizeText(sentence)).find();
	}

	private static String normalizeText(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ");
	}

	private static String normalizedLocalizedText(ProviderWordInfo.LocalizedText text) {
		return normalizeLanguageValues(text.en()) + "|" + normalizeLanguageValues(text.ru());
	}

	private static String normalizeLanguageValues(List<String> values) {
		return values.stream().map(ProviderWordInfoValidator::normalizeText).map(String::toLowerCase)
				.sorted().reduce((left, right) -> left + "\\u001F" + right).orElse("");
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
