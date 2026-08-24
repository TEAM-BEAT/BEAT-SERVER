package com.beat.support.security.jwt.internal

sealed interface AccessTokenAuthenticationResult {

    data class Authenticated(
        val memberId: Long,
        val roleName: String,
    ) : AccessTokenAuthenticationResult

    data class Rejected(
        val failure: AccessTokenAuthenticationFailure,
    ) : AccessTokenAuthenticationResult
}
