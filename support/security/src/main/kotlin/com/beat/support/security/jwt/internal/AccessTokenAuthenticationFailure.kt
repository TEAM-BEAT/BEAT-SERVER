package com.beat.support.security.jwt.internal

enum class AccessTokenAuthenticationFailure {
    EXPIRED,
    INVALID_TOKEN,
    INVALID_SIGNATURE,
    UNSUPPORTED,
    EMPTY,
}
