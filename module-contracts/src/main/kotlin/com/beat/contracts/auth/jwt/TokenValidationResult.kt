package com.beat.contracts.auth.jwt

enum class TokenValidationResult {
    VALID,
    INVALID_SIGNATURE,
    INVALID_TOKEN,
    EXPIRED,
    UNSUPPORTED,
    EMPTY,
}
