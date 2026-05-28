package com.vocavista.backend.dictionary;

class DictionaryValidationException extends RuntimeException {

	DictionaryValidationException(String message) {
		super(message);
	}

}

class DictionaryNotFoundException extends RuntimeException {

	DictionaryNotFoundException(String message) {
		super(message);
	}

}
