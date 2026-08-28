package com.beat.application.frontoffice.security

enum class TokenAuthenticationFailure {
    EXPIRED,
    INVALID_TOKEN,
    INVALID_SIGNATURE,
    UNSUPPORTED,
    EMPTY,
}
