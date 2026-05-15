package com.beat.gateway.jwt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.beat.contracts.auth.JwtSubject;
import com.beat.contracts.auth.JwtTokenType;
import com.beat.contracts.auth.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String STRONG_BASE64_SECRET =
		"AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QA==";
	private static final String KEY_ID = "test-current";
	private static final Instant NOW = Instant.parse("2099-05-15T00:00:00Z");
	private static final long ACCESS_TTL_MILLIS = 3_600_000L;
	private static final long REFRESH_TTL_MILLIS = 1_209_600_000L;

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = provider(STRONG_BASE64_SECRET);
	}

	@Test
	void validBase64StrongSecretIssuesAndValidatesAccessTokenWithTokenTypeAndKid() {
		String accessToken = jwtTokenProvider.issueAccessToken(subject());

		Jws<Claims> parsed = parseCurrent(accessToken);

		assertEquals(KEY_ID, parsed.getHeader().getKeyId());
		assertEquals("ACCESS", parsed.getPayload().get("tokenType", String.class));
		assertEquals(TokenValidationResult.VALID, jwtTokenProvider.validateAccessToken(accessToken));
		assertEquals(1L, jwtTokenProvider.getMemberId(accessToken, JwtTokenType.ACCESS));
		assertEquals("ROLE_MEMBER", jwtTokenProvider.getRoleName(accessToken, JwtTokenType.ACCESS));
	}

	@Test
	void validBase64StrongSecretIssuesAndValidatesRefreshTokenWithTokenTypeAndKid() {
		String refreshToken = jwtTokenProvider.issueRefreshToken(subject());

		Jws<Claims> parsed = parseCurrent(refreshToken);

		assertEquals(KEY_ID, parsed.getHeader().getKeyId());
		assertEquals("REFRESH", parsed.getPayload().get("tokenType", String.class));
		assertEquals(TokenValidationResult.VALID, jwtTokenProvider.validateRefreshToken(refreshToken));
	}

	@Test
	void refreshTokenFailsAccessValidation() {
		String refreshToken = jwtTokenProvider.issueRefreshToken(subject());

		assertEquals(TokenValidationResult.INVALID_TOKEN, jwtTokenProvider.validateAccessToken(refreshToken));
	}

	@Test
	void accessTokenFailsRefreshValidation() {
		String accessToken = jwtTokenProvider.issueAccessToken(subject());

		assertEquals(TokenValidationResult.INVALID_TOKEN, jwtTokenProvider.validateRefreshToken(accessToken));
	}

	@Test
	void startupValidationRequiresKeyId() {
		JwtTokenProvider provider = provider(STRONG_BASE64_SECRET, "", ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS);

		assertThrows(IllegalStateException.class, provider::validateSigningKeyConfiguration);
	}

	@Test
	void startupValidationRequiresPositiveAccessTokenTtl() {
		JwtTokenProvider provider = provider(STRONG_BASE64_SECRET, KEY_ID, 0L, REFRESH_TTL_MILLIS);

		assertThrows(IllegalStateException.class, provider::validateSigningKeyConfiguration);
	}

	@Test
	void malformedBase64SecretFailsIssuance() {
		JwtTokenProvider provider = provider("not-base64!!");

		assertThrows(RuntimeException.class, () -> provider.issueAccessToken(subject()));
	}

	@Test
	void weakDecodedKeyFailsIssuance() {
		JwtTokenProvider provider = provider(Base64.getEncoder().encodeToString("weak".getBytes()));

		assertThrows(WeakKeyException.class, () -> provider.issueAccessToken(subject()));
	}

	@Test
	void nonNumericMemberIdClaimFailsValidationBeforeExtraction() {
		String token = Jwts.builder()
			.header()
			.keyId(KEY_ID)
			.and()
			.issuedAt(Date.from(NOW))
			.expiration(Date.from(NOW.plusMillis(ACCESS_TTL_MILLIS)))
			.claim("memberId", "not-a-number")
			.claim("role", "ROLE_MEMBER")
			.claim("tokenType", "ACCESS")
			.signWith(currentKey())
			.compact();

		assertEquals(TokenValidationResult.INVALID_TOKEN, jwtTokenProvider.validateAccessToken(token));
	}

	private JwtTokenProvider provider(String secret) {
		return provider(secret, KEY_ID, ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS);
	}

	private JwtTokenProvider provider(
		String secret,
		String keyId,
		long accessTokenExpireTime,
		long refreshTokenExpireTime
	) {
		return new JwtTokenProvider(new JwtProperties(
			secret,
			accessTokenExpireTime,
			refreshTokenExpireTime,
			keyId
		));
	}

	private Jws<Claims> parseCurrent(String token) {
		return Jwts.parser()
			.verifyWith(currentKey())
			.build()
			.parseSignedClaims(token);
	}

	private SecretKey currentKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(STRONG_BASE64_SECRET));
	}

	private JwtSubject subject() {
		return new JwtSubject(1L, "ROLE_MEMBER");
	}

}
