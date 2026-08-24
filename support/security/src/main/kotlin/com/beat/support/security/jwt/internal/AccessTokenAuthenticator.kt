package com.beat.support.security.jwt.internal

/**
 * Access token 1회 파싱으로 인증 결과를 반환하는 gateway 내부 계약.
 *
 * `JwtAuthenticationFilter`가 토큰 1개로 memberId/roleName을 한 번에 얻기 위한
 * security-web 전용 계약이다.
 */
fun interface AccessTokenAuthenticator {

    fun authenticateAccessToken(token: String): AccessTokenAuthenticationResult
}
