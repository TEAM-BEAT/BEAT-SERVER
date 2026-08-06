package com.beat.admin.exception

import com.beat.domain.exception.DomainErrorType
import com.beat.domain.exception.DomainException
import com.beat.global.support.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.filter.ServerHttpObservationFilter
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class AdminGlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(exception: DomainException): ResponseEntity<ErrorResponse> {
        val status = toV1DomainStatus(exception)
        log.info("Domain failure: code={}, status={}", exception.errorCode.code, status.value())
        return ResponseEntity.status(status)
            .body(ErrorResponse.of(status.value(), toV1DomainMessage(exception)))
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val message = ex.bindingResult.fieldError?.defaultMessage ?: "Validation error"
        return handleExceptionInternal(
            ex, ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message),
            headers, HttpStatus.BAD_REQUEST, request,
        )
    }

    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = handleExceptionInternal(
        ex,
        ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Missing required parameter: ${ex.parameterName}"),
        headers,
        HttpStatus.BAD_REQUEST,
        request,
    )

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (ex !is MethodArgumentTypeMismatchException) return clientErrorResponse(ex, status, headers, request)
        val requiredType = ex.requiredType?.simpleName ?: "Unknown Type"
        val message = "Invalid value for parameter: ${ex.name} (Expected: $requiredType)"
        return handleExceptionInternal(
            ex, ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message),
            headers, HttpStatus.BAD_REQUEST, request,
        )
    }

    override fun handleServletRequestBindingException(
        ex: ServletRequestBindingException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (ex !is MissingRequestCookieException) return clientErrorResponse(ex, status, headers, request)
        return handleExceptionInternal(
            ex,
            ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Missing required cookie: ${ex.cookieName}"),
            headers,
            HttpStatus.BAD_REQUEST,
            request,
        )
    }

    public override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = ResponseEntity(null, headers, status)

    public override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            log.error("Unexpected Spring MVC error: ", ex)
            if (request is ServletWebRequest) markObservationError(request.request, ex)
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    override fun createResponseEntity(
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val responseBody = if (body is ErrorResponse) {
            body
        } else {
            val message = if (statusCode.is5xxServerError) {
                "서버 내부 오류입니다."
            } else {
                "잘못된 요청입니다."
            }
            ErrorResponse.of(statusCode.value(), message)
        }
        return super.createResponseEntity(responseBody, headers, statusCode, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Unexpected server error: ", exception)
        markObservationError(request, exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다."))
    }

    @ExceptionHandler(AdminApplicationException::class)
    fun handleApplicationException(
        exception: AdminApplicationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorCode = exception.errorCode
        val status = toHttpStatus(errorCode.getType())
        when {
            errorCode.getType() == ApplicationErrorType.INTERNAL_ERROR -> {
                log.error("Application failure: code={}, status={}", errorCode.getCode(), status.value(), exception)
                markObservationError(request, exception)
            }
            status.is5xxServerError ->
                log.error("Upstream application failure: code={}, status={}", errorCode.getCode(), status.value())
            else -> log.info("Application failure: code={}, status={}", errorCode.getCode(), status.value())
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), errorCode.getMessage()))
    }

    private fun clientErrorResponse(
        exception: Exception,
        status: HttpStatusCode,
        headers: HttpHeaders,
        request: WebRequest,
    ): ResponseEntity<Any>? = handleExceptionInternal(
        exception, ErrorResponse.of(status.value(), "잘못된 요청입니다."), headers, status, request,
    )

    private fun markObservationError(request: HttpServletRequest, exception: Exception) {
        ServerHttpObservationFilter.findObservationContext(request).ifPresent { it.setError(exception) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminGlobalExceptionHandler::class.java)

        private fun toV1DomainMessage(exception: DomainException): String = when (exception.errorCode.code) {
            "BOOKING_INVALID_PURCHASE_TICKET_COUNT",
            "BOOKING_INVALID_REFUND_ACCOUNT",
            "BOOKING_PAYMENT_CONFIRMATION_NOT_ALLOWED",
            "BOOKING_REFUND_REQUEST_NOT_ALLOWED",
            "PERFORMANCE_NON_POSITIVE_RUNNING_TIME",
            "PERFORMANCE_NEGATIVE_SCHEDULE_COUNT",
            "SCHEDULE_INVALID_BOOKING_WINDOW",
            "SCHEDULE_NEGATIVE_TICKET_COUNT",
            "SCHEDULE_NON_POSITIVE_TICKET_COUNT",
            -> "잘못된 데이터 형식입니다."
            "SCHEDULE_TOO_MANY_SCHEDULES" -> "공연 회차는 최대 10개까지 추가할 수 있습니다."
            "SCHEDULE_ALLOCATED_TICKETS_EXCEED_TOTAL" ->
                "판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다."
            else -> exception.errorCode.message
        }

        private fun toV1DomainStatus(exception: DomainException): HttpStatus = when (exception.errorCode.code) {
            "BOOKING_PAYMENT_CONFIRMATION_NOT_ALLOWED",
            "BOOKING_REFUND_REQUEST_NOT_ALLOWED",
            -> HttpStatus.BAD_REQUEST
            else -> when (exception.errorCode.type) {
                DomainErrorType.INVALID_INPUT -> HttpStatus.BAD_REQUEST
                DomainErrorType.STATE_CONFLICT -> HttpStatus.CONFLICT
            }
        }

        private fun toHttpStatus(type: ApplicationErrorType): HttpStatus = when (type) {
            ApplicationErrorType.INVALID_INPUT -> HttpStatus.BAD_REQUEST
            ApplicationErrorType.UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED
            ApplicationErrorType.FORBIDDEN -> HttpStatus.FORBIDDEN
            ApplicationErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            ApplicationErrorType.STATE_CONFLICT -> HttpStatus.CONFLICT
            ApplicationErrorType.UPSTREAM_FAILURE -> HttpStatus.BAD_GATEWAY
            ApplicationErrorType.UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
            ApplicationErrorType.UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT
            ApplicationErrorType.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}