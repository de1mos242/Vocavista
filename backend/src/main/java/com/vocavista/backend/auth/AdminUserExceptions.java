package com.vocavista.backend.auth;

class AdminUserValidationException extends RuntimeException {

	AdminUserValidationException(String message) {
		super(message);
	}

}

class AdminUserNotFoundException extends RuntimeException {

	AdminUserNotFoundException(String message) {
		super(message);
	}

}
