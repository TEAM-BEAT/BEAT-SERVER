package com.beat.support.security.token

sealed interface TokenAuthenticationResult {

    data class Authenticated(
        val subject: TokenSubject,
    ) : TokenAuthenticationResult

    data class Rejected(
        val failure: TokenAuthenticationFailure,
    ) : TokenAuthenticationResult
}
