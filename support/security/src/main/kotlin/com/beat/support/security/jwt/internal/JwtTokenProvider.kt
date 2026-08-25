package com.beat.support.security.jwt.internal

import com.beat.application.frontoffice.security.RefreshTokenAuthenticator
import com.beat.application.frontoffice.security.TokenAuthenticationFailure
import com.beat.application.frontoffice.security.TokenAuthenticationResult
import com.beat.application.frontoffice.security.TokenIssuer
import com.beat.application.frontoffice.security.TokenSubject
import com.beat.support.security.access.AccessTokenAuthenticationFailure
import com.beat.support.security.access.AccessTokenAuthenticationResult
import com.beat.support.security.access.AccessTokenAuthenticator
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.PrematureJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException

/** 발급/파싱은 [JwtTokenIssuer]·[JwtTokenParser]에 위임하고, JWT 검증 결과를 각 consumer-owned contract로 변환한다. */
internal class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val jwtTokenParser: JwtTokenParser,
) : TokenIssuer, RefreshTokenAuthenticator, AccessTokenAuthenticator {

    override fun issueAccessToken(subject: TokenSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.accessTokenExpireTime, JwtTokenType.ACCESS)

    override fun issueRefreshToken(subject: TokenSubject): String =
        jwtTokenIssuer.issue(subject, jwtProperties.refreshTokenExpireTime, JwtTokenType.REFRESH)

    override fun authenticateAccessToken(token: String): AccessTokenAuthenticationResult =
        authenticateToken(
            token = token,
            expectedType = JwtTokenType.ACCESS,
            onAuthenticated = { memberId, roleName ->
                AccessTokenAuthenticationResult.Authenticated(memberId, roleName)
            },
            onRejected = { failure ->
                AccessTokenAuthenticationResult.Rejected(failure.toAccessFailure())
            },
        )

    override fun authenticateRefreshToken(token: String): TokenAuthenticationResult =
        authenticateToken(
            token = token,
            expectedType = JwtTokenType.REFRESH,
            onAuthenticated = { memberId, roleName ->
                TokenAuthenticationResult.Authenticated(TokenSubject(memberId, roleName))
            },
            onRejected = { failure ->
                TokenAuthenticationResult.Rejected(failure.toApplicationFailure())
            },
        )

    /**
     * JWT 파싱/claim 검증과 예외 정규화는 이 한 곳에서 수행한다. Access/Refresh consumer는 정규화된 결과를 각자 소유한 contract로만
     * 변환한다.
     *
     * [InvalidTokenClaimsException]은 [IllegalArgumentException]의 하위 타입이므로 catch 순서를 유지해야 한다.
     */
    private fun <T> authenticateToken(
        token: String,
        expectedType: JwtTokenType,
        onAuthenticated: (memberId: Long, roleName: String) -> T,
        onRejected: (JwtAuthenticationFailure) -> T,
    ): T =
        try {
            val claims = jwtTokenParser.parse(token, expectedType)
            val memberId = claims.memberIdOrNull()
            val roleName = claims.roleNameOrNull()

            if (memberId == null || roleName == null) {
                onRejected(JwtAuthenticationFailure.INVALID_TOKEN)
            } else {
                onAuthenticated(memberId, roleName)
            }
        } catch (_: MalformedJwtException) {
            onRejected(JwtAuthenticationFailure.INVALID_TOKEN)
        } catch (_: ExpiredJwtException) {
            onRejected(JwtAuthenticationFailure.EXPIRED)
        } catch (_: UnsupportedJwtException) {
            onRejected(JwtAuthenticationFailure.UNSUPPORTED)
        } catch (_: InvalidTokenClaimsException) {
            onRejected(JwtAuthenticationFailure.INVALID_TOKEN)
        } catch (_: IllegalArgumentException) {
            onRejected(JwtAuthenticationFailure.EMPTY)
        } catch (_: SignatureException) {
            onRejected(JwtAuthenticationFailure.INVALID_SIGNATURE)
        } catch (_: PrematureJwtException) {
            onRejected(JwtAuthenticationFailure.INVALID_TOKEN)
        } catch (_: JwtException) {
            onRejected(JwtAuthenticationFailure.INVALID_TOKEN)
        }

    private fun JwtAuthenticationFailure.toAccessFailure(): AccessTokenAuthenticationFailure =
        when (this) {
            JwtAuthenticationFailure.EXPIRED -> AccessTokenAuthenticationFailure.EXPIRED
            JwtAuthenticationFailure.INVALID_TOKEN -> AccessTokenAuthenticationFailure.INVALID_TOKEN
            JwtAuthenticationFailure.INVALID_SIGNATURE ->
                AccessTokenAuthenticationFailure.INVALID_SIGNATURE
            JwtAuthenticationFailure.UNSUPPORTED -> AccessTokenAuthenticationFailure.UNSUPPORTED
            JwtAuthenticationFailure.EMPTY -> AccessTokenAuthenticationFailure.EMPTY
        }

    private fun JwtAuthenticationFailure.toApplicationFailure(): TokenAuthenticationFailure =
        when (this) {
            JwtAuthenticationFailure.EXPIRED -> TokenAuthenticationFailure.EXPIRED
            JwtAuthenticationFailure.INVALID_TOKEN -> TokenAuthenticationFailure.INVALID_TOKEN
            JwtAuthenticationFailure.INVALID_SIGNATURE ->
                TokenAuthenticationFailure.INVALID_SIGNATURE
            JwtAuthenticationFailure.UNSUPPORTED -> TokenAuthenticationFailure.UNSUPPORTED
            JwtAuthenticationFailure.EMPTY -> TokenAuthenticationFailure.EMPTY
        }

    private fun Claims.memberIdOrNull(): Long? =
        this[JwtClaimNames.MEMBER_ID]?.toString()?.toLongOrNull()

    private fun Claims.roleNameOrNull(): String? =
        get(JwtClaimNames.ROLE, String::class.java)?.takeIf(String::isNotBlank)

    private enum class JwtAuthenticationFailure {
        EXPIRED,
        INVALID_TOKEN,
        INVALID_SIGNATURE,
        UNSUPPORTED,
        EMPTY,
    }
}
