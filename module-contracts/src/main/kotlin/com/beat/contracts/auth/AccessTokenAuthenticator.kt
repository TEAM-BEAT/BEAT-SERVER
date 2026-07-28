package com.beat.contracts.auth

fun interface AccessTokenAuthenticator {

    fun authenticateAccessToken(token: String): AccessTokenAuthenticationResult
}

sealed interface AccessTokenAuthenticationResult {

    data class Authenticated(
        val memberId: Long,
        val roleName: String,
    ) : AccessTokenAuthenticationResult

    data class Rejected(
        val validationResult: TokenValidationResult,
    ) : AccessTokenAuthenticationResult
}
