package com.beat.support.security.token

enum class TokenAuthenticationFailure {
    EXPIRED,
    INVALID_TOKEN,
    INVALID_SIGNATURE,
    UNSUPPORTED,
    EMPTY,
}
