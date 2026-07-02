package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Optional;

public record ProviderWordInfo(
		InputLanguage inputLanguage,
		List<WordMeaning> meanings) {

	public record WordMeaning(
			String normalizedWord,
			Language language,
			LocalizedText translations,
			ProviderPartOfSpeech partOfSpeech,
			Optional<ProviderGender> gender,
			Optional<ProviderArticle> article,
			Optional<String> plural,
			ProviderFrequency frequency,
			Boolean isCompound,
			List<CompoundPart> compoundParts,
			LocalizedText shortNote,
			List<WordExample> examples) {
	}

	public record LocalizedText(List<String> en, List<String> ru) {
	}

	public record CompoundPart(String word, LocalizedText meanings) {
	}

	public record WordExample(String sentence, LocalizedText translations) {
	}

	public enum Language {
		de
	}

	public enum InputLanguage {
		en, ru, de
	}

	public enum ProviderPartOfSpeech {
		noun, verb, adjective, adverb, pronoun, preposition, conjunction, interjection, phrase, other
	}

	public enum ProviderGender {
		masculine, feminine, neuter
	}

	public enum ProviderArticle {
		der, die, das
	}

	public enum ProviderFrequency {
		rare, uncommon, common, very_common
	}

}
