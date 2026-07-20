package com.beat.admin.exception

enum class ApplicationErrorType {
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
