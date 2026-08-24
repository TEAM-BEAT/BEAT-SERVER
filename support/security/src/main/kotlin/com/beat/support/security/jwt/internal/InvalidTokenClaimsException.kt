package com.beat.support.security.jwt.internal

/**
 * 서명은 유효하지만 claim 계약(예: `tokenType` 불일치)을 만족하지 않는 경우.
 *
 * 기존 client 계약을 보존하기 위해 [IllegalArgumentException]을 상속한다.
 * (`JwtAuthenticationFilter`가 `IllegalArgumentException`을 401로 매핑한다.)
 */
internal class InvalidTokenClaimsException(message: String) : IllegalArgumentException(message)
