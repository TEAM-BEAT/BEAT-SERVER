package com.beat.gateway.jwt.internal

import com.beat.contracts.auth.JwtTokenType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory

/**
 * JWT 서명 검증과 `tokenType` claim 계약 검증만 담당한다.
 * 검증 실패는 예외로 올리고, 결과 코드 변환은 [JwtTokenProvider]가 수행한다.
 */
class JwtTokenParser(
    private val signingKeyHolder: JwtSigningKeyHolder,
) {

    private val log = LoggerFactory.getLogger(JwtTokenParser::class.java)

    fun parse(token: String, expectedType: JwtTokenType): Claims {
        val claims = Jwts.parser()
            .verifyWith(signingKeyHolder.signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

        requireExpectedTokenType(claims, expectedType)
        log.debug("JWT validated with current key for expected tokenType={}", expectedType)
        return claims
    }

    private fun requireExpectedTokenType(claims: Claims, expectedType: JwtTokenType) {
        val actualType = claims.get(JwtClaimNames.TOKEN_TYPE, String::class.java)
        if (expectedType.name != actualType) {
            log.warn("JWT tokenType mismatch: expected={}, actual={}", expectedType, actualType)
            throw InvalidTokenClaimsException("JWT tokenType does not match expected type")
        }
    }
}
