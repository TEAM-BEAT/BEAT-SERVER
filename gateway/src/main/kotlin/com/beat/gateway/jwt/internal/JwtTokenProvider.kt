package com.beat.gateway.jwt.internal

import com.beat.contracts.auth.JwtSubject
import com.beat.contracts.auth.JwtTokenPort
import com.beat.contracts.auth.JwtTokenType
import com.beat.contracts.auth.TokenValidationResult
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException

/**
 * [JwtTokenPort] 어댑터. 발급/파싱은 [JwtTokenIssuer]·[JwtTokenParser]에 위임하고,
 * 이 클래스는 예외를 client 계약인 [TokenValidationResult]로 변환하는 책임만 갖는다.
 */
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val jwtTokenParser: JwtTokenParser,
) : JwtTokenPort {

    override fun issueAccessToken(subject: JwtSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.accessTokenExpireTime, JwtTokenType.ACCESS)

    override fun issueRefreshToken(subject: JwtSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.refreshTokenExpireTime, JwtTokenType.REFRESH)

    override fun validateAccessToken(token: String): TokenValidationResult =
        validate(token, JwtTokenType.ACCESS)

    override fun validateRefreshToken(token: String): TokenValidationResult =
        validate(token, JwtTokenType.REFRESH)

    override fun getMemberId(token: String, expectedType: JwtTokenType): Long {
        val claims = jwtTokenParser.parse(token, expectedType)
        return claims.memberIdOrNull()
            ?: throw IllegalArgumentException("JWT does not contain memberId claim")
    }

    override fun getRoleName(token: String, expectedType: JwtTokenType): String {
        val claims = jwtTokenParser.parse(token, expectedType)
        return claims.roleNameOrNull()
            ?: throw IllegalArgumentException("JWT does not contain role claim")
    }

    /**
     * 예외 catch 순서에 의존한다. [InvalidTokenClaimsException]은 [IllegalArgumentException]의
     * 하위 타입이므로 반드시 먼저 잡아야 `INVALID_TOKEN`으로 분류된다.
     */
    private fun validate(token: String, expectedType: JwtTokenType): TokenValidationResult = try {
        val claims = jwtTokenParser.parse(token, expectedType)

        when {
            claims.memberIdOrNull() == null -> {
                TokenValidationResult.INVALID_TOKEN
            }

            claims.roleNameOrNull() == null -> {
                TokenValidationResult.INVALID_TOKEN
            }

            else -> TokenValidationResult.VALID
        }
    } catch (_: MalformedJwtException) {
        TokenValidationResult.INVALID_TOKEN
    } catch (_: ExpiredJwtException) {
        TokenValidationResult.EXPIRED
    } catch (_: UnsupportedJwtException) {
        TokenValidationResult.UNSUPPORTED
    } catch (_: InvalidTokenClaimsException) {
        TokenValidationResult.INVALID_TOKEN
    } catch (_: IllegalArgumentException) {
        TokenValidationResult.EMPTY
    } catch (_: SignatureException) {
        TokenValidationResult.INVALID_SIGNATURE
    }

    private fun Claims.memberIdOrNull(): Long? = this[JwtClaimNames.MEMBER_ID]?.toString()?.toLongOrNull()

    private fun Claims.roleNameOrNull(): String? =
        get(JwtClaimNames.ROLE, String::class.java)?.takeIf(String::isNotBlank)
}
