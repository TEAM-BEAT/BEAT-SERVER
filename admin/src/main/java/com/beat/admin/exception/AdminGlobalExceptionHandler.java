package com.beat.admin.exception;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.beat.domain.exception.DomainException;
import com.beat.global.support.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@RestControllerAdvice
public class AdminGlobalExceptionHandler extends ResponseEntityExceptionHandler {
	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ErrorResponse> handleDomainException(final DomainException exception) {
		HttpStatus status = toV1DomainStatus(exception);
		log.info("Domain failure: code={}, status={}", exception.getErrorCode().getCode(), status.value());
		return ResponseEntity.status(status)
			.body(ErrorResponse.of(status.value(), toV1DomainMessage(exception)));
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		String errorMessage = Optional.ofNullable(exception.getBindingResult().getFieldError())
			.map(FieldError::getDefaultMessage)
			.orElse("Validation error");
		return super.handleExceptionInternal(exception,
			ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errorMessage), headers, HttpStatus.BAD_REQUEST, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleMissingServletRequestParameter(
		MissingServletRequestParameterException exception,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		String message = String.format("Missing required parameter: %s", exception.getParameterName());
		return super.handleExceptionInternal(exception,
			ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message), headers, HttpStatus.BAD_REQUEST, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleTypeMismatch(
		TypeMismatchException exception,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		if (!(exception instanceof MethodArgumentTypeMismatchException methodArgumentException)) {
			return clientErrorResponse(exception, status, headers, request);
		}
		String requiredType = methodArgumentException.getRequiredType() != null
			? methodArgumentException.getRequiredType().getSimpleName()
			: "Unknown Type";
		return super.handleExceptionInternal(exception,
			ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
				"Invalid value for parameter: " + methodArgumentException.getName() + " (Expected: " + requiredType
					+ ")"),
			headers, HttpStatus.BAD_REQUEST, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleServletRequestBindingException(
		ServletRequestBindingException exception,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		if (exception instanceof MissingRequestCookieException missingCookieException) {
			String message = String.format("Missing required cookie: %s", missingCookieException.getCookieName());
			return super.handleExceptionInternal(exception,
				ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message), headers, HttpStatus.BAD_REQUEST, request);
		}
		return clientErrorResponse(exception, status, headers, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleNoResourceFoundException(
		NoResourceFoundException exception,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		return new ResponseEntity<>(null, headers, status);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleExceptionInternal(
		Exception exception,
		@Nullable Object body,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		if (status.is5xxServerError()) {
			log.error("Unexpected Spring MVC error: ", exception);
			if (request instanceof ServletWebRequest servletWebRequest) {
				ServerHttpObservationFilter.findObservationContext(servletWebRequest.getRequest())
					.ifPresent(context -> context.setError(exception));
			}
		}
		return super.handleExceptionInternal(exception, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(
		@Nullable Object body,
		HttpHeaders headers,
		HttpStatusCode status,
		WebRequest request
	) {
		Object responseBody = body;
		if (!(body instanceof ErrorResponse)) {
			String message = status.is5xxServerError() ? "서버 내부 오류입니다." : "잘못된 요청입니다.";
			responseBody = ErrorResponse.of(status.value(), message);
		}
		return super.createResponseEntity(responseBody, headers, status, request);
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleException(final Exception exception, HttpServletRequest request) {
		log.error("Unexpected server error: ", exception);
		ServerHttpObservationFilter.findObservationContext(request)
			.ifPresent(context -> context.setError(exception));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다."));
	}

	@ExceptionHandler(AdminApplicationException.class)
	public ResponseEntity<ErrorResponse> handleApplicationException(
		final AdminApplicationException exception,
		final HttpServletRequest request
	) {
		ApplicationErrorType type = exception.getErrorCode().getType();
		HttpStatus status = toHttpStatus(type);
		if (type == ApplicationErrorType.INTERNAL_ERROR) {
			log.error("Application failure: code={}, status={}",
				exception.getErrorCode().getCode(), status.value(), exception);
			ServerHttpObservationFilter.findObservationContext(request)
				.ifPresent(context -> context.setError(exception));
		} else if (status.is5xxServerError()) {
			log.error("Upstream application failure: code={}, status={}",
				exception.getErrorCode().getCode(), status.value());
		} else {
			log.info("Application failure: code={}, status={}",
				exception.getErrorCode().getCode(), status.value());
		}
		return ResponseEntity.status(status)
			.body(ErrorResponse.of(status.value(), exception.getErrorCode().getMessage()));
	}

	private static String toV1DomainMessage(DomainException exception) {
		return switch (exception.getErrorCode().getCode()) {
			case "BOOKING_INVALID_PURCHASE_TICKET_COUNT",
			     "BOOKING_INVALID_REFUND_ACCOUNT",
			     "BOOKING_PAYMENT_CONFIRMATION_NOT_ALLOWED",
			     "BOOKING_REFUND_REQUEST_NOT_ALLOWED",
			     "PERFORMANCE_NON_POSITIVE_RUNNING_TIME",
			     "PERFORMANCE_NEGATIVE_SCHEDULE_COUNT",
			     "SCHEDULE_INVALID_BOOKING_WINDOW",
			     "SCHEDULE_NEGATIVE_TICKET_COUNT",
			     "SCHEDULE_NON_POSITIVE_TICKET_COUNT" -> "잘못된 데이터 형식입니다.";
			case "SCHEDULE_TOO_MANY_SCHEDULES" -> "공연 회차는 최대 10개까지 추가할 수 있습니다.";
			case "SCHEDULE_ALLOCATED_TICKETS_EXCEED_TOTAL" -> "판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다.";
			default -> exception.getErrorCode().getMessage();
		};
	}

	private static HttpStatus toV1DomainStatus(DomainException exception) {
		return switch (exception.getErrorCode().getCode()) {
			case "BOOKING_PAYMENT_CONFIRMATION_NOT_ALLOWED",
			     "BOOKING_REFUND_REQUEST_NOT_ALLOWED" -> HttpStatus.BAD_REQUEST;
			default -> switch (exception.getErrorCode().getType()) {
				case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
				case STATE_CONFLICT -> HttpStatus.CONFLICT;
			};
		};
	}

	private static HttpStatus toHttpStatus(ApplicationErrorType type) {
		return switch (type) {
			case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
			case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case STATE_CONFLICT -> HttpStatus.CONFLICT;
			case UPSTREAM_FAILURE -> HttpStatus.BAD_GATEWAY;
			case UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
			case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
			case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}

	private @Nullable ResponseEntity<Object> clientErrorResponse(
		Exception exception,
		HttpStatusCode status,
		HttpHeaders headers,
		WebRequest request
	) {
		return super.handleExceptionInternal(exception,
			ErrorResponse.of(status.value(), "잘못된 요청입니다."), headers, status, request);
	}
}
