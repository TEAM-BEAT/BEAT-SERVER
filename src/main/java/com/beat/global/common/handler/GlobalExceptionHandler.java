package com.beat.global.common.handler;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.BeatException;
import com.beat.global.common.exception.ConflictException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;
import com.beat.global.common.exception.UnauthorizedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 400 BAD_REQUEST
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequestException(final BadRequestException e) {
		log.warn("BadRequestException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.from(e.getBaseErrorCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		String errorMessage = Optional.ofNullable(e.getBindingResult().getFieldError())
			.map(FieldError::getDefaultMessage)  // 메서드 참조 사용
			.orElse("Validation error");

		log.warn("MethodArgumentNotValidException: {}", errorMessage);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errorMessage));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
		MissingServletRequestParameterException e) {
		log.warn("MissingServletRequestParameterException: {}", e.getMessage());
		String message = String.format("Missing required parameter: %s", e.getParameterName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {
		log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
		String errorMessage = "Invalid value for parameter: " + e.getName();
		String requiredType = "Unknown Type";
		if (e.getRequiredType() != null) {
			requiredType = e.getRequiredType().getSimpleName();
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errorMessage + " (Expected: " + requiredType + ")"));
	}

	@ExceptionHandler(MissingRequestCookieException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(MissingRequestCookieException e) {
		log.warn("MissingRequestCookieException: {}", e.getMessage());
		String message = String.format("Missing required cookie: %s", e.getCookieName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
	}

	/**
	 * 401 UNAUTHORIZED
	 */
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedException(final UnauthorizedException e) {
		log.warn("UnauthorizedException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.from(e.getBaseErrorCode()));
	}

	/**
	 * 403 FORBIDDEN
	 */
	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbiddenException(final ForbiddenException e) {
		log.warn("ForbiddenException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.from(e.getBaseErrorCode()));
	}

	/**
	 * 404 NOT_FOUND
	 */
	@ExceptionHandler(NotFoundException.class)
	protected ResponseEntity<ErrorResponse> handleNotFoundException(final NotFoundException e) {
		log.warn("NotFoundException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.from(e.getBaseErrorCode()));
	}

	/**
	 * 409 CONFLICT
	 */
	@ExceptionHandler(ConflictException.class)
	protected ResponseEntity<ErrorResponse> handleConflictException(final ConflictException e) {
		log.warn("ConflictException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.from(e.getBaseErrorCode()));
	}

	/**
	 * 500 INTERNAL_SERVER
	 */
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleException(final Exception e) {
		log.error("Unexpected server error: ", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다."));
	}

	/**
	 * CUSTOM_ERROR
	 */
	@ExceptionHandler(BeatException.class)
	public ResponseEntity<ErrorResponse> handleBeatException(final BeatException e) {
		log.warn("BeatException occurred: ", e);
		return ResponseEntity.status(e.getBaseErrorCode().getStatus()).body(ErrorResponse.from(e.getBaseErrorCode()));
	}
}
