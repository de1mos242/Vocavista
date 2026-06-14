package com.vocavista.backend.media.pronunciation;

import com.vocavista.backend.api.model.ErrorResponse;
import static com.vocavista.backend.web.ApiErrorResponses.error;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = PronunciationController.class)
@Slf4j
class MediaErrorHandler {

	@ExceptionHandler({ PronunciationValidationException.class, MethodArgumentNotValidException.class,
			ConstraintViolationException.class, HandlerMethodValidationException.class, HttpMessageNotReadableException.class })
	ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
		log.debug("Invalid media request", ex);
		return error(HttpStatus.BAD_REQUEST, "invalid_request", "Invalid pronunciation request");
	}

	@ExceptionHandler(PronunciationNotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(PronunciationNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, "not_found", "Pronunciation asset was not found");
	}

	@ExceptionHandler(MediaGenerationException.class)
	ResponseEntity<ErrorResponse> handleProviderFailure(MediaGenerationException ex) {
		log.warn("Media provider failed", ex);
		return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getCode(), ex.getMessage());
	}

}
