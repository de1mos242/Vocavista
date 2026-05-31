package com.vocavista.backend.auth;

import com.vocavista.backend.api.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = AdminController.class)
class AdminErrorHandler {

	@ExceptionHandler({ AdminUserValidationException.class, MethodArgumentNotValidException.class,
			ConstraintViolationException.class, HandlerMethodValidationException.class, HttpMessageNotReadableException.class })
	ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
		return error(HttpStatus.BAD_REQUEST, "invalid_request", "Invalid admin user request");
	}

	@ExceptionHandler(AdminUserNotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(AdminUserNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, "not_found", "User account was not found");
	}

	private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(code, message));
	}

}
