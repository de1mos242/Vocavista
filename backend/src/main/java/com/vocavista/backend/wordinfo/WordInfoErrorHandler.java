package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
@Slf4j
class WordInfoErrorHandler {

	@ExceptionHandler({ WordInfoValidationException.class, MissingServletRequestParameterException.class,
			ConstraintViolationException.class, HandlerMethodValidationException.class })
	ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
		log.debug("Invalid word info request", ex);
		return error(HttpStatus.BAD_REQUEST, "invalid_request", "Invalid word request");
	}

	@ExceptionHandler(AiProviderBadGatewayException.class)
	ResponseEntity<ErrorResponse> handleBadGateway(AiProviderBadGatewayException ex) {
		log.warn("AI provider returned an invalid response", ex);
		return error(HttpStatus.BAD_GATEWAY, "ai_provider_error", "AI provider returned an invalid response");
	}

	@ExceptionHandler(AiProviderUnavailableException.class)
	ResponseEntity<ErrorResponse> handleUnavailable(AiProviderUnavailableException ex) {
		log.warn("AI provider is unavailable", ex);
		return error(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_unavailable", "AI provider is unavailable");
	}

	private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(code, message));
	}

}
