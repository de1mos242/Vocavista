package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Map;

public record ProviderWordInfo(
		String normalizedWord,
		String language,
		Map<String, List<String>> translations,
		String partOfSpeech,
		String gender,
		String article,
		String plural,
		String frequency,
		Boolean isCompound,
		List<CompoundPart> compoundParts,
		Map<String, List<String>> shortNote,
		List<WordExample> examples) {

	public record CompoundPart(String word, Map<String, List<String>> meanings) {
	}

	public record WordExample(String sentence, Map<String, List<String>> translations) {
	}

}
