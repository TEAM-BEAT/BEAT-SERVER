package com.beat.application.admin.exception

class AdminApplicationException(
    val errorCode: AdminApplicationErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)

interface AdminApplicationErrorCode {
    val code: String
    val type: AdminApplicationErrorType
    val message: String
}

enum class AdminApplicationErrorType {
    INVALID_INPUT,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    STATE_CONFLICT,
    UPSTREAM_FAILURE,
    UPSTREAM_UNAVAILABLE,
    UPSTREAM_TIMEOUT,
    INTERNAL_ERROR,
}
