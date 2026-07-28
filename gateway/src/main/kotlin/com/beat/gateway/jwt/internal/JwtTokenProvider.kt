package com.beat.gateway.jwt.internal

import com.beat.contracts.auth.AccessTokenAuthenticationResult
import com.beat.contracts.auth.AccessTokenAuthenticator
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
) : JwtTokenPort, AccessTokenAuthenticator {

    override fun issueAccessToken(subject: JwtSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.accessTokenExpireTime, JwtTokenType.ACCESS)

    override fun issueRefreshToken(subject: JwtSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.refreshTokenExpireTime, JwtTokenType.REFRESH)

    override fun validateAccessToken(token: String): TokenValidationResult =
        when (val result = authenticateAccessToken(token)) {
            is AccessTokenAuthenticationResult.Authenticated -> TokenValidationResult.VALID
            is AccessTokenAuthenticationResult.Rejected -> result.validationResult
        }

    override fun validateRefreshToken(token: String): TokenValidationResult =
        validate(token, JwtTokenType.REFRESH)

    override fun authenticateAccessToken(token: String): AccessTokenAuthenticationResult =
        mapParsedToken(
            token = token,
            expectedType = JwtTokenType.ACCESS,
            onValid = { claims ->
                val memberId = claims.memberIdOrNull()
                val roleName = claims.roleNameOrNull()

                if (memberId == null || roleName == null) {
                    AccessTokenAuthenticationResult.Rejected(TokenValidationResult.INVALID_TOKEN)
                } else {
                    AccessTokenAuthenticationResult.Authenticated(memberId, roleName)
                }
            },
            onInvalid = AccessTokenAuthenticationResult::Rejected,
        )

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
    private fun validate(token: String, expectedType: JwtTokenType): TokenValidationResult =
        mapParsedToken(
            token = token,
            expectedType = expectedType,
            onValid = { claims ->
                if (claims.memberIdOrNull() == null || claims.roleNameOrNull() == null) {
                    TokenValidationResult.INVALID_TOKEN
                } else {
                    TokenValidationResult.VALID
                }
            },
            onInvalid = { it },
        )

    private fun <T> mapParsedToken(
        token: String,
        expectedType: JwtTokenType,
        onValid: (Claims) -> T,
        onInvalid: (TokenValidationResult) -> T,
    ): T = try {
        onValid(jwtTokenParser.parse(token, expectedType))
    } catch (_: MalformedJwtException) {
        onInvalid(TokenValidationResult.INVALID_TOKEN)
    } catch (_: ExpiredJwtException) {
        onInvalid(TokenValidationResult.EXPIRED)
    } catch (_: UnsupportedJwtException) {
        onInvalid(TokenValidationResult.UNSUPPORTED)
    } catch (_: InvalidTokenClaimsException) {
        onInvalid(TokenValidationResult.INVALID_TOKEN)
    } catch (_: IllegalArgumentException) {
        onInvalid(TokenValidationResult.EMPTY)
    } catch (_: SignatureException) {
        onInvalid(TokenValidationResult.INVALID_SIGNATURE)
    }

    private fun Claims.memberIdOrNull(): Long? = this[JwtClaimNames.MEMBER_ID]?.toString()?.toLongOrNull()

    private fun Claims.roleNameOrNull(): String? =
        get(JwtClaimNames.ROLE, String::class.java)?.takeIf(String::isNotBlank)
}
