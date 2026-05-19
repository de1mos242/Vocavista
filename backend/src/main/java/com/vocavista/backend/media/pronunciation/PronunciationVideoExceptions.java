package com.vocavista.backend.media.pronunciation;

class PronunciationVideoValidationException extends RuntimeException {

	PronunciationVideoValidationException(String message) {
		super(message);
	}

}

class PronunciationVideoNotFoundException extends RuntimeException {

	PronunciationVideoNotFoundException(String message) {
		super(message);
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
