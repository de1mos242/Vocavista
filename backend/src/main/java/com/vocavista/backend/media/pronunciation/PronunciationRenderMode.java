package com.vocavista.backend.media.pronunciation;

import org.springframework.util.StringUtils;

enum PronunciationRenderMode {

	VEO_VIDEO("veo-video"),
	TALKING_HEAD("talking-head");

	private final String apiValue;

	PronunciationRenderMode(String apiValue) {
		this.apiValue = apiValue;
	}

	String apiValue() {
		return apiValue;
	}

	static PronunciationRenderMode fromApiValue(String value) {
		if (!StringUtils.hasText(value)) {
			throw new PronunciationValidationException("renderMode is required");
		}
		for (PronunciationRenderMode mode : values()) {
			if (mode.apiValue.equals(value)) {
				return mode;
			}
		}
		throw new PronunciationValidationException("renderMode must be veo-video or talking-head");
	}

}
