package com.vocavista.backend.media.pronunciation;

record PronunciationScript(
		String word,
		String phrase,
		String language,
		String text,
		String templateVersion,
		String voiceConfig,
		String avatarConfig) {
}
