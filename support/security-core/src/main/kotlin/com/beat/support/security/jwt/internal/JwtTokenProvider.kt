package com.beat.support.security.jwt.internal

import com.beat.application.frontoffice.security.RefreshTokenAuthenticator
import com.beat.application.frontoffice.security.TokenAuthenticationFailure
import com.beat.application.frontoffice.security.TokenAuthenticationResult
import com.beat.application.frontoffice.security.TokenIssuer
import com.beat.application.frontoffice.security.TokenSubject
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException

/**
 * 발급/파싱은 [JwtTokenIssuer]·[JwtTokenParser]에 위임하고,
 * 이 클래스는 예외를 [TokenAuthenticationResult]로 변환하는 책임만 갖는다.
 */
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val jwtTokenParser: JwtTokenParser,
) : TokenIssuer, RefreshTokenAuthenticator, AccessTokenAuthenticator {

    override fun issueAccessToken(subject: TokenSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.accessTokenExpireTime, JwtTokenType.ACCESS)

    override fun issueRefreshToken(subject: TokenSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.refreshTokenExpireTime, JwtTokenType.REFRESH)

    override fun authenticateAccessToken(token: String): TokenAuthenticationResult =
        mapParsedToken(
            token = token,
            expectedType = JwtTokenType.ACCESS,
            onValid = { claims -> claims.toAuthenticationResult() },
            onInvalid = TokenAuthenticationResult::Rejected,
        )

    override fun authenticateRefreshToken(token: String): TokenAuthenticationResult =
        mapParsedToken(
            token = token,
            expectedType = JwtTokenType.REFRESH,
            onValid = { claims -> claims.toAuthenticationResult() },
            onInvalid = TokenAuthenticationResult::Rejected,
        )

    /**
     * 예외 catch 순서에 의존한다. [InvalidTokenClaimsException]은 [IllegalArgumentException]의
     * 하위 타입이므로 반드시 먼저 잡아야 `INVALID_TOKEN`으로 분류된다.
     */
    private fun mapParsedToken(
        token: String,
        expectedType: JwtTokenType,
        onValid: (Claims) -> TokenAuthenticationResult,
        onInvalid: (TokenAuthenticationFailure) -> TokenAuthenticationResult,
    ): TokenAuthenticationResult = try {
        onValid(jwtTokenParser.parse(token, expectedType))
    } catch (_: MalformedJwtException) {
        onInvalid(TokenAuthenticationFailure.INVALID_TOKEN)
    } catch (_: ExpiredJwtException) {
        onInvalid(TokenAuthenticationFailure.EXPIRED)
    } catch (_: UnsupportedJwtException) {
        onInvalid(TokenAuthenticationFailure.UNSUPPORTED)
    } catch (_: InvalidTokenClaimsException) {
        onInvalid(TokenAuthenticationFailure.INVALID_TOKEN)
    } catch (_: IllegalArgumentException) {
        onInvalid(TokenAuthenticationFailure.EMPTY)
    } catch (_: SignatureException) {
        onInvalid(TokenAuthenticationFailure.INVALID_SIGNATURE)
    }

    private fun Claims.toAuthenticationResult(): TokenAuthenticationResult {
        val memberId = memberIdOrNull()
        val roleName = roleNameOrNull()
        return if (memberId == null || roleName == null) {
            TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN)
        } else {
            TokenAuthenticationResult.Authenticated(TokenSubject(memberId, roleName))
        }
    }

    private fun Claims.memberIdOrNull(): Long? = this[JwtClaimNames.MEMBER_ID]?.toString()?.toLongOrNull()

    private fun Claims.roleNameOrNull(): String? =
        get(JwtClaimNames.ROLE, String::class.java)?.takeIf(String::isNotBlank)
}
