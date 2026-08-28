package com.beat.application.frontoffice.security

sealed interface TokenAuthenticationResult {
    data class Authenticated(val subject: TokenSubject) : TokenAuthenticationResult

    data class Rejected(val failure: TokenAuthenticationFailure) : TokenAuthenticationResult
}
