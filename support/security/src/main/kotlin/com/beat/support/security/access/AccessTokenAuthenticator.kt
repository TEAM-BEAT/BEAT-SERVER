package com.beat.support.security.access

/**
 * Servlet security가 access token을 1회 검증하고 인증 주체를 얻기 위한 support:security의 Web-facing SPI.
 *
 * Application contract와 분리해 support:security-web이 application:frontoffice를 직접 의존하지 않도록 한다.
 */
fun interface AccessTokenAuthenticator {

    fun authenticateAccessToken(token: String): AccessTokenAuthenticationResult
}
