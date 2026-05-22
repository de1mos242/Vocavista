package com.vocavista.backend.wordinfo;

class WordInfoValidationException extends RuntimeException {

	WordInfoValidationException(String message) {
		super(message);
	}

}

class AiProviderBadGatewayException extends RuntimeException {

	private final Object providerResponse;

	AiProviderBadGatewayException(String message) {
		super(message);
		this.providerResponse = null;
	}

	AiProviderBadGatewayException(String message, Throwable cause) {
		super(message, cause);
		this.providerResponse = null;
	}

	AiProviderBadGatewayException(String message, Throwable cause, Object providerResponse) {
		super(message, cause);
		this.providerResponse = providerResponse;
	}

	Object providerResponse() {
		return providerResponse;
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
