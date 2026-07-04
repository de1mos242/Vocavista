package com.vocavista.backend.vocabulary;

import java.util.Locale;

public final class VocabularyText {

	private VocabularyText() {
	}

	public static String optional(String value) {
		return value == null ? null : value.trim().replaceAll("\\s+", " ");
	}

	public static String languageCode(String value) {
		String normalized = optional(value);
		return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
	}

}
