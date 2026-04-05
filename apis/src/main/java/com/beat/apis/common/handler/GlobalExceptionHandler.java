package com.beat.apis.common.handler;

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
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequestException(final BadRequestException exception) {
		log.warn("BadRequestException: {}", exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.from(exception.getBaseErrorCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception) {
		String errorMessage = Optional.ofNullable(exception.getBindingResult().getFieldError())
			.map(FieldError::getDefaultMessage)
			.orElse("Validation error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errorMessage));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
		MissingServletRequestParameterException exception
	) {
		String message = String.format("Missing required parameter: %s", exception.getParameterName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException exception
	) {
		String requiredType =
			exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "Unknown Type";
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
				"Invalid value for parameter: " + exception.getName() + " (Expected: " + requiredType + ")")
		);
	}

	@ExceptionHandler(MissingRequestCookieException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(MissingRequestCookieException exception) {
		String message = String.format("Missing required cookie: %s", exception.getCookieName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedException(final UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.from(exception.getBaseErrorCode()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbiddenException(final ForbiddenException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.from(exception.getBaseErrorCode()));
	}

	@ExceptionHandler(NotFoundException.class)
	protected ResponseEntity<ErrorResponse> handleNotFoundException(final NotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.from(exception.getBaseErrorCode()));
	}

	@ExceptionHandler(ConflictException.class)
	protected ResponseEntity<ErrorResponse> handleConflictException(final ConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.from(exception.getBaseErrorCode()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	protected ResponseEntity<Void> handleNoResourceFoundException(final NoResourceFoundException exception) {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleException(final Exception exception) {
		log.error("Unexpected server error: ", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다."));
	}

	@ExceptionHandler(BeatException.class)
	public ResponseEntity<ErrorResponse> handleBeatException(final BeatException exception) {
		return ResponseEntity.status(exception.getBaseErrorCode().getStatus())
			.body(ErrorResponse.from(exception.getBaseErrorCode()));
	}
}
