package com.beat.gateway.jwt.internal

import com.beat.contracts.auth.jwt.TokenValidationResult

/**
 * Access token 1회 파싱으로 인증 결과를 반환하는 gateway 내부 계약.
 *
 * [com.beat.contracts.auth.jwt.JwtTokenPort]는 발급/검증/claim 추출을 실행 모듈에 노출하는
 * 모듈 간 계약이다. 반면 이 타입은 `JwtAuthenticationFilter`가 토큰 1개로 memberId/roleName을
 * 한 번에 얻기 위한 gateway 내부 계약이므로 `internal`에 둔다.
 */
fun interface AccessTokenAuthenticator {

    fun authenticateAccessToken(token: String): AccessTokenAuthenticationResult
}
