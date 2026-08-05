package com.beat.gateway.jwt.internal

import com.beat.contracts.auth.jwt.JwtSubject
import com.beat.contracts.auth.jwt.JwtTokenType
import com.beat.contracts.auth.jwt.TokenValidationResult
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.WeakKeyException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as given

class JwtTokenProviderTest {

    private val jwtTokenProvider = provider()

    @Test
    fun `access token은 kid와 tokenType을 담아 발급되고 검증된다`() {
        val accessToken = jwtTokenProvider.issueAccessToken(subject())

        val parsed = parse(accessToken)

        assertAll(
            { assertEquals(KEY_ID, parsed.header.keyId) },
            { assertEquals("ACCESS", parsed.payload.get(JwtClaimNames.TOKEN_TYPE, String::class.java)) },
            { assertEquals(TokenValidationResult.VALID, jwtTokenProvider.validateAccessToken(accessToken)) },
            { assertEquals(1L, jwtTokenProvider.getMemberId(accessToken, JwtTokenType.ACCESS)) },
            { assertEquals("ROLE_MEMBER", jwtTokenProvider.getRoleName(accessToken, JwtTokenType.ACCESS)) },
        )
    }

    @Test
    fun `access token 인증은 한 번 파싱한 claims에서 사용자 정보를 추출한다`() {
        val properties = JwtProperties(STRONG_BASE64_SECRET, ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS, KEY_ID)
        val signingKeyHolder = JwtSigningKeyHolder(properties)
        val parser = mock(JwtTokenParser::class.java)
        val claims = mock(Claims::class.java)
        val provider = JwtTokenProvider(properties, JwtTokenIssuer(signingKeyHolder), parser)
        given(parser.parse("access-token", JwtTokenType.ACCESS)).thenReturn(claims)
        given(claims[JwtClaimNames.MEMBER_ID]).thenReturn(1L)
        given(claims.get(JwtClaimNames.ROLE, String::class.java)).thenReturn("ROLE_MEMBER")

        val result = provider.authenticateAccessToken("access-token")

        assertEquals(AccessTokenAuthenticationResult.Authenticated(1L, "ROLE_MEMBER"), result)
        verify(parser).parse("access-token", JwtTokenType.ACCESS)
    }

    @Test
    fun `refresh token은 kid와 tokenType을 담아 발급되고 검증된다`() {
        val refreshToken = jwtTokenProvider.issueRefreshToken(subject())

        val parsed = parse(refreshToken)

        assertAll(
            { assertEquals(KEY_ID, parsed.header.keyId) },
            { assertEquals("REFRESH", parsed.payload.get(JwtClaimNames.TOKEN_TYPE, String::class.java)) },
            { assertEquals(TokenValidationResult.VALID, jwtTokenProvider.validateRefreshToken(refreshToken)) },
        )
    }

    @Test
    fun `refresh token은 access 검증을 통과하지 못한다`() {
        val refreshToken = jwtTokenProvider.issueRefreshToken(subject())

        assertEquals(TokenValidationResult.INVALID_TOKEN, jwtTokenProvider.validateAccessToken(refreshToken))
    }

    @Test
    fun `access token은 refresh 검증을 통과하지 못한다`() {
        val accessToken = jwtTokenProvider.issueAccessToken(subject())

        assertEquals(TokenValidationResult.INVALID_TOKEN, jwtTokenProvider.validateRefreshToken(accessToken))
    }

    @Test
    fun `Base64가 아닌 secret은 발급에 실패한다`() {
        val provider = provider(secret = "not-base64!!")

        assertThrows<RuntimeException> { provider.issueAccessToken(subject()) }
    }

    @Test
    fun `HS256 최소 강도를 만족하지 못하는 secret은 발급에 실패한다`() {
        val provider = provider(secret = Base64.getEncoder().encodeToString("weak".toByteArray()))

        assertThrows<WeakKeyException> { provider.issueAccessToken(subject()) }
    }

    @Test
    fun `숫자가 아닌 memberId claim은 추출 전에 검증에서 걸러진다`() {
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

        assertEquals(TokenValidationResult.INVALID_TOKEN, jwtTokenProvider.validateAccessToken(token))
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

    private fun subject(): JwtSubject = JwtSubject(1L, "ROLE_MEMBER")

    companion object {
        private const val STRONG_BASE64_SECRET =
            "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QA=="
        private const val KEY_ID = "test-current"
        private val NOW: Instant = Instant.parse("2099-05-15T00:00:00Z")
        private const val ACCESS_TTL_MILLIS = 3_600_000L
        private const val REFRESH_TTL_MILLIS = 1_209_600_000L
    }
}
