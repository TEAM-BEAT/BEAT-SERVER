package com.beat.support.security.jwt.internal

import com.beat.support.security.token.TokenAuthenticationFailure
import com.beat.support.security.token.TokenAuthenticationResult
import com.beat.support.security.token.TokenSubject
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.WeakKeyException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class JwtTokenProviderTest : FunSpec() {

    private val jwtTokenProvider = provider()

    init {
        isolationMode = IsolationMode.SingleInstance

        test("access token은 kid와 tokenType을 담아 발급되고 검증된다") {
            val accessToken = jwtTokenProvider.issueAccessToken(subject())

            val parsed = parse(accessToken)

            parsed.header.keyId shouldBe KEY_ID
            parsed.payload.get(JwtClaimNames.TOKEN_TYPE, String::class.java) shouldBe "ACCESS"
            jwtTokenProvider.authenticateAccessToken(accessToken) shouldBe
                TokenAuthenticationResult.Authenticated(subject())
        }

        test("access token 인증은 한 번 파싱한 claims에서 사용자 정보를 추출한다") {
            val properties = JwtProperties(STRONG_BASE64_SECRET, ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS, KEY_ID)
            val signingKeyHolder = JwtSigningKeyHolder(properties)
            val parser = mockk<JwtTokenParser>(relaxed = true)
            val claims = mockk<Claims>(relaxed = true)
            val provider = JwtTokenProvider(properties, JwtTokenIssuer(signingKeyHolder), parser)
            every { parser.parse("access-token", JwtTokenType.ACCESS) } returns claims
            every { claims[JwtClaimNames.MEMBER_ID] } returns 1L
            every { claims.get(JwtClaimNames.ROLE, String::class.java) } returns "ROLE_MEMBER"

            val result = provider.authenticateAccessToken("access-token")

            result shouldBe TokenAuthenticationResult.Authenticated(TokenSubject(1L, "ROLE_MEMBER"))
            verify { parser.parse("access-token", JwtTokenType.ACCESS) }
        }

        test("refresh token은 kid와 tokenType을 담아 발급되고 검증된다") {
            val refreshToken = jwtTokenProvider.issueRefreshToken(subject())

            val parsed = parse(refreshToken)

            parsed.header.keyId shouldBe KEY_ID
            parsed.payload.get(JwtClaimNames.TOKEN_TYPE, String::class.java) shouldBe "REFRESH"
            jwtTokenProvider.authenticateRefreshToken(refreshToken) shouldBe
                TokenAuthenticationResult.Authenticated(subject())
        }

        test("refresh token은 access 검증을 통과하지 못한다") {
            val refreshToken = jwtTokenProvider.issueRefreshToken(subject())

            jwtTokenProvider.authenticateAccessToken(refreshToken) shouldBe
                TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN)
        }

        test("access token은 refresh 검증을 통과하지 못한다") {
            val accessToken = jwtTokenProvider.issueAccessToken(subject())

            jwtTokenProvider.authenticateRefreshToken(accessToken) shouldBe
                TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN)
        }

        test("Base64가 아닌 secret은 발급에 실패한다") {
            val provider = provider(secret = "not-base64!!")

            shouldThrow<RuntimeException> { provider.issueAccessToken(subject()) }
        }

        test("HS256 최소 강도를 만족하지 못하는 secret은 발급에 실패한다") {
            val provider = provider(secret = Base64.getEncoder().encodeToString("weak".toByteArray()))

            shouldThrow<WeakKeyException> { provider.issueAccessToken(subject()) }
        }

        test("숫자가 아닌 memberId claim은 추출 전에 검증에서 걸러진다") {
            val token = Jwts.builder()
                .header()
                .keyId(KEY_ID)
                .and()
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plusMillis(ACCESS_TTL_MILLIS)))
                .claim(JwtClaimNames.MEMBER_ID, "not-a-number")
                .claim(JwtClaimNames.ROLE, "ROLE_MEMBER")
                .claim(JwtClaimNames.TOKEN_TYPE, "ACCESS")
                .signWith(strongKey())
                .compact()

            jwtTokenProvider.authenticateAccessToken(token) shouldBe
                TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN)
        }
    }

    private fun provider(secret: String = STRONG_BASE64_SECRET): JwtTokenProvider {
        val properties = JwtProperties(secret, ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS, KEY_ID)
        val signingKeyHolder = JwtSigningKeyHolder(properties)
        return JwtTokenProvider(
            properties,
            JwtTokenIssuer(signingKeyHolder),
            JwtTokenParser(signingKeyHolder),
        )
    }

    private fun parse(token: String): Jws<Claims> =
        Jwts.parser()
            .verifyWith(strongKey())
            .build()
            .parseSignedClaims(token)

    private fun strongKey(): SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(STRONG_BASE64_SECRET))

    private fun subject(): TokenSubject = TokenSubject(1L, "ROLE_MEMBER")

    companion object {
        private const val STRONG_BASE64_SECRET =
            "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QA=="
        private const val KEY_ID = "test-current"
        private val NOW: Instant = Instant.parse("2099-05-15T00:00:00Z")
        private const val ACCESS_TTL_MILLIS = 3_600_000L
        private const val REFRESH_TTL_MILLIS = 1_209_600_000L
    }
}
