package com.beat.application.frontoffice.security

interface RefreshTokenAuthenticator {
    fun authenticateRefreshToken(token: String): TokenAuthenticationResult
}
