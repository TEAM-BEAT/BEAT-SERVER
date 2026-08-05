package com.beat.gateway.jwt.internal

import com.beat.contracts.auth.jwt.TokenValidationResult

sealed interface AccessTokenAuthenticationResult {

    data class Authenticated(
        val memberId: Long,
        val roleName: String,
    ) : AccessTokenAuthenticationResult

    data class Rejected(
        val validationResult: TokenValidationResult,
    ) : AccessTokenAuthenticationResult
}
