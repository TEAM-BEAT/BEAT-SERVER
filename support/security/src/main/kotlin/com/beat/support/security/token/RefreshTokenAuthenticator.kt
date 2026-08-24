package com.beat.support.security.token

interface RefreshTokenAuthenticator {

    fun authenticateRefreshToken(token: String): TokenAuthenticationResult
}
