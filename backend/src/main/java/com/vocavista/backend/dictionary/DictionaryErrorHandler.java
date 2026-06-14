package com.vocavista.backend.dictionary;

import com.vocavista.backend.api.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = DictionaryController.class)
@Slf4j
class DictionaryErrorHandler {

	@ExceptionHandler({ DictionaryValidationException.class, MethodArgumentNotValidException.class,
			ConstraintViolationException.class, HandlerMethodValidationException.class, HttpMessageNotReadableException.class })
	ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
		log.debug("Invalid dictionary request", ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("invalid_request", "Invalid dictionary request"));
	}

	@ExceptionHandler(DictionaryNotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(DictionaryNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse("not_found", "Dictionary entry was not found"));
	}

}
