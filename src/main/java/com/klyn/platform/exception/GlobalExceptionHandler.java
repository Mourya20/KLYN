package com.klyn.platform.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(DuplicateResourceException.class)
	ResponseEntity<ApiError> duplicate(DuplicateResourceException exception, HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiError> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
		return response(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> constraint(ConstraintViolationException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> authentication(AuthenticationException exception, HttpServletRequest request) {
		return response(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}

	private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
				message, request.getRequestURI()));
	}
}