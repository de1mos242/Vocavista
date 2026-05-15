package com.vocavista.backend.wordinfo;

class WordInfoValidationException extends RuntimeException {

	WordInfoValidationException(String message) {
		super(message);
	}

}

class AiProviderBadGatewayException extends RuntimeException {

	AiProviderBadGatewayException(String message) {
		super(message);
	}

	AiProviderBadGatewayException(String message, Throwable cause) {
		super(message, cause);
	}

}

class AiProviderUnavailableException extends RuntimeException {

	AiProviderUnavailableException(String message) {
		super(message);
	}

	AiProviderUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
