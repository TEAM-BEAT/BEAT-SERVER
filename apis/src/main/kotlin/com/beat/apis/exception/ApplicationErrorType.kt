package com.beat.apis.exception

enum class ApplicationErrorType {
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
