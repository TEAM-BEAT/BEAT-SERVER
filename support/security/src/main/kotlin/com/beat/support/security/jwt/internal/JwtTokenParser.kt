package com.beat.support.security.jwt.internal

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts

/** JWT 서명 검증과 `tokenType` claim 계약 검증만 담당한다. 검증 실패는 예외로 올리고, 결과 코드 변환은 [JwtTokenProvider]가 수행한다. */
internal class JwtTokenParser(private val signingKeyHolder: JwtSigningKeyHolder) {

    fun parse(token: String, expectedType: JwtTokenType): Claims {
        val claims =
            Jwts.parser()
                .verifyWith(signingKeyHolder.signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

        requireExpectedTokenType(claims, expectedType)
        return claims
    }

    private fun requireExpectedTokenType(claims: Claims, expectedType: JwtTokenType) {
        val actualType = claims.get(JwtClaimNames.TOKEN_TYPE, String::class.java)
        if (expectedType.name != actualType) {
            throw InvalidTokenClaimsException("JWT tokenType does not match expected type")
        }
    }
}
