package com.beat.apis.exception

import com.beat.apis.response.ErrorResponse
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
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
class ApiGlobalExceptionHandler : ResponseEntityExceptionHandler() {
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

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? = ResponseEntity(null, headers, status)

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            log.error(ex) { "Unexpected Spring MVC error: " }
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
            val message = if (statusCode.is5xxServerError) "서버 내부 오류입니다." else "잘못된 요청입니다."
            ErrorResponse.of(statusCode.value(), message)
        }
        return super.createResponseEntity(responseBody, headers, statusCode, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error(exception) { "Unexpected server error: " }
        markObservationError(request, exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다."))
    }

    @ExceptionHandler(FrontofficeApplicationException::class)
    fun handleFrontofficeApplicationException(
        exception: FrontofficeApplicationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorCode = exception.errorCode
        val status = toHttpStatus(errorCode.type)
        when {
            errorCode.type == FrontofficeApplicationErrorType.INTERNAL_ERROR -> {
                log.error(exception) { "Application failure: code=${errorCode.code}, status=${status.value()}" }
                markObservationError(request, exception)
            }
            status.is5xxServerError -> log.error { "Upstream application failure: code=${errorCode.code}, status=${status.value()}" }
            else -> log.info { "Application failure: code=${errorCode.code}, status=${status.value()}" }
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), errorCode.message))
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
        private val log = KotlinLogging.logger {}

        fun toHttpStatus(type: FrontofficeApplicationErrorType): HttpStatus = when (type) {
            FrontofficeApplicationErrorType.INVALID_INPUT -> HttpStatus.BAD_REQUEST
            FrontofficeApplicationErrorType.UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED
            FrontofficeApplicationErrorType.FORBIDDEN -> HttpStatus.FORBIDDEN
            FrontofficeApplicationErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            FrontofficeApplicationErrorType.STATE_CONFLICT -> HttpStatus.CONFLICT
            FrontofficeApplicationErrorType.UPSTREAM_FAILURE -> HttpStatus.BAD_GATEWAY
            FrontofficeApplicationErrorType.UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
            FrontofficeApplicationErrorType.UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT
            FrontofficeApplicationErrorType.RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS
            FrontofficeApplicationErrorType.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }

    }
}
