package com.beat.support.security.access

enum class AccessTokenAuthenticationFailure {
    EXPIRED,
    INVALID_TOKEN,
    INVALID_SIGNATURE,
    UNSUPPORTED,
    EMPTY,
}
