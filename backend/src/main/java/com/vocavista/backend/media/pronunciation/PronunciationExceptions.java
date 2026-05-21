package com.vocavista.backend.media.pronunciation;

class PronunciationValidationException extends RuntimeException {

	PronunciationValidationException(String message) {
		super(message);
	}

}

class PronunciationNotFoundException extends RuntimeException {

	PronunciationNotFoundException(String message) {
		super(message);
	}

	PronunciationNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}

class MediaGenerationException extends RuntimeException {

	private final String code;

	MediaGenerationException(String code, String message) {
		super(message);
		this.code = code;
	}

	MediaGenerationException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	String getCode() {
		return code;
	}

}
