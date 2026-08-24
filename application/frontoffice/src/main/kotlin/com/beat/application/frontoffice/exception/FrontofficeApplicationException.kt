package com.beat.application.frontoffice.exception

class FrontofficeApplicationException(
    val errorCode: FrontofficeApplicationErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)

interface FrontofficeApplicationErrorCode {
    val code: String
    val type: FrontofficeApplicationErrorType
    val message: String
}

enum class FrontofficeApplicationErrorType {
    INVALID_INPUT,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    STATE_CONFLICT,
    UPSTREAM_FAILURE,
    UPSTREAM_UNAVAILABLE,
    UPSTREAM_TIMEOUT,
    RATE_LIMITED,
    INTERNAL_ERROR,
}
