package com.beat.gateway.jwt.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String STRONG_BASE64_SECRET =
		"AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QA==";
	private static final String LEGACY_RAW_SECRET = "legacy-secret-at-least-256-bits-long-for-compatibility";
	private static final String KEY_ID = "test-current";
	private static final Instant NOW = Instant.parse("2099-05-15T00:00:00Z");
	private static final Instant LEGACY_CUTOFF_IN_FUTURE = Instant.parse("2999-05-15T00:00:00Z");
	private static final Instant LEGACY_CUTOFF_IN_PAST = Instant.parse("1970-01-01T00:00:00Z");
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
	void currentTokensUseDecodedBase64KeyInsteadOfLegacyDoubleBase64Key() {
		String accessToken = jwtTokenProvider.issueAccessToken(subject());

		String legacySignedToken = legacyToken(NOW, NOW.plusMillis(ACCESS_TTL_MILLIS));

		assertNotEquals(accessToken, legacySignedToken);
		assertEquals(TokenValidationResult.INVALID_SIGNATURE,
			provider(STRONG_BASE64_SECRET).validateRefreshToken(legacySignedToken));
	}

	@Test
	void legacyAccessTokenPassesBeforeCutoff() {
		JwtTokenProvider providerBeforeCutoff = providerBeforeCutoff(STRONG_BASE64_SECRET, STRONG_BASE64_SECRET);
		String legacyAccessToken = legacyToken(NOW, NOW.plusMillis(ACCESS_TTL_MILLIS));

		assertEquals(TokenValidationResult.VALID, providerBeforeCutoff.validateAccessToken(legacyAccessToken));
	}

	@Test
	void legacyAccessTokenCanUsePreviousRawSecretWhenCurrentSecretRotates() {
		JwtTokenProvider providerBeforeCutoff = providerBeforeCutoff(STRONG_BASE64_SECRET, LEGACY_RAW_SECRET);
		String legacyAccessToken = legacyToken(LEGACY_RAW_SECRET, NOW, NOW.plusMillis(ACCESS_TTL_MILLIS));

		assertEquals(TokenValidationResult.VALID, providerBeforeCutoff.validateAccessToken(legacyAccessToken));
		assertThrows(IllegalStateException.class,
			() -> providerBeforeCutoff(STRONG_BASE64_SECRET).validateAccessToken(legacyAccessToken));
	}

	@Test
	void legacyAccessTokenFailsAfterCutoff() {
		JwtTokenProvider providerAfterCutoff = provider(STRONG_BASE64_SECRET);
		String legacyAccessToken = legacyToken(NOW, NOW.plusMillis(ACCESS_TTL_MILLIS));

		assertEquals(TokenValidationResult.INVALID_SIGNATURE, providerAfterCutoff.validateAccessToken(legacyAccessToken));
	}

	@Test
	void legacyRefreshLikeTokenWithLongLifetimeFailsAccessValidationBeforeCutoff() {
		JwtTokenProvider providerBeforeCutoff = providerBeforeCutoff(STRONG_BASE64_SECRET, STRONG_BASE64_SECRET);
		String legacyRefreshLikeToken = legacyToken(NOW, NOW.plusMillis(REFRESH_TTL_MILLIS));

		assertEquals(TokenValidationResult.INVALID_TOKEN, providerBeforeCutoff.validateAccessToken(legacyRefreshLikeToken));
	}

	@Test
	void legacyFallbackRequiresLegacySecretBeforeCutoff() {
		JwtTokenProvider providerBeforeCutoff = providerBeforeCutoff(STRONG_BASE64_SECRET);
		String legacyAccessToken = legacyToken(NOW, NOW.plusMillis(ACCESS_TTL_MILLIS));

		assertThrows(IllegalStateException.class, () -> providerBeforeCutoff.validateAccessToken(legacyAccessToken));
	}

	@Test
	void startupValidationRequiresLegacySecretWhenFallbackCutoffIsInFuture() {
		JwtTokenProvider providerBeforeCutoff = providerBeforeCutoff(STRONG_BASE64_SECRET);

		assertThrows(IllegalStateException.class, providerBeforeCutoff::validateSigningKeyConfiguration);
	}

	@Test
	void startupValidationChecksLegacySigningKeyStrengthWhenFallbackCutoffIsInFuture() {
		JwtTokenProvider providerBeforeCutoff = providerBeforeCutoff(STRONG_BASE64_SECRET, "weak");

		assertThrows(WeakKeyException.class, providerBeforeCutoff::validateSigningKeyConfiguration);
	}

	@Test
	void startupValidationAllowsBlankLegacySecretWhenFallbackCutoffIsInPast() {
		JwtTokenProvider providerAfterCutoff = provider(STRONG_BASE64_SECRET);

		assertDoesNotThrow(providerAfterCutoff::validateSigningKeyConfiguration);
	}

	@Test
	void startupValidationRequiresKeyId() {
		JwtTokenProvider provider = provider(STRONG_BASE64_SECRET, "", "", ACCESS_TTL_MILLIS,
			REFRESH_TTL_MILLIS, LEGACY_CUTOFF_IN_PAST);

		assertThrows(IllegalStateException.class, provider::validateSigningKeyConfiguration);
	}

	@Test
	void startupValidationRequiresPositiveAccessTokenTtl() {
		JwtTokenProvider provider = provider(STRONG_BASE64_SECRET, "", KEY_ID, 0L,
			REFRESH_TTL_MILLIS, LEGACY_CUTOFF_IN_PAST);

		assertThrows(IllegalStateException.class, provider::validateSigningKeyConfiguration);
	}

	@Test
	void startupValidationRequiresLegacyCutoff() {
		JwtTokenProvider provider = provider(STRONG_BASE64_SECRET, "", KEY_ID, ACCESS_TTL_MILLIS,
			REFRESH_TTL_MILLIS, null);

		assertThrows(IllegalStateException.class, provider::validateSigningKeyConfiguration);
	}

	@Test
	void malformedBase64SecretFailsIssuance() {
		JwtTokenProvider provider = providerBeforeCutoff("not-base64!!");

		assertThrows(RuntimeException.class, () -> provider.issueAccessToken(subject()));
	}

	@Test
	void weakDecodedKeyFailsIssuance() {
		JwtTokenProvider provider = providerBeforeCutoff(Base64.getEncoder().encodeToString("weak".getBytes()));

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
		return provider(secret, "", LEGACY_CUTOFF_IN_PAST);
	}

	private JwtTokenProvider providerBeforeCutoff(String secret) {
		return provider(secret, "", LEGACY_CUTOFF_IN_FUTURE);
	}

	private JwtTokenProvider providerBeforeCutoff(String secret, String legacySecret) {
		return provider(secret, legacySecret, LEGACY_CUTOFF_IN_FUTURE);
	}

	private JwtTokenProvider provider(String secret, String legacySecret, Instant legacyCutoff) {
		return provider(secret, legacySecret, KEY_ID, ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS, legacyCutoff);
	}

	private JwtTokenProvider provider(
		String secret,
		String legacySecret,
		String keyId,
		long accessTokenExpireTime,
		long refreshTokenExpireTime,
		Instant legacyCutoff
	) {
		return new JwtTokenProvider(new JwtProperties(
			secret,
			legacySecret,
			accessTokenExpireTime,
			refreshTokenExpireTime,
			keyId,
			legacyCutoff
		));
	}

	private Jws<Claims> parseCurrent(String token) {
		return Jwts.parser()
			.verifyWith(currentKey())
			.build()
			.parseSignedClaims(token);
	}

	private String legacyToken(Instant issuedAt, Instant expiration) {
		return legacyToken(STRONG_BASE64_SECRET, issuedAt, expiration);
	}

	private String legacyToken(String signingSecret, Instant issuedAt, Instant expiration) {
		return Jwts.builder()
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiration))
			.claim("memberId", 1L)
			.claim("role", "ROLE_MEMBER")
			.signWith(legacyKey(signingSecret))
			.compact();
	}

	private SecretKey currentKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(STRONG_BASE64_SECRET));
	}

	private SecretKey legacyKey() {
		return legacyKey(STRONG_BASE64_SECRET);
	}

	private SecretKey legacyKey(String signingSecret) {
		String onceEncoded = Base64.getEncoder()
			.encodeToString(signingSecret.getBytes(StandardCharsets.UTF_8));
		String twiceEncoded = Base64.getEncoder()
			.encodeToString(onceEncoded.getBytes(StandardCharsets.UTF_8));
		return Keys.hmacShaKeyFor(twiceEncoded.getBytes(StandardCharsets.UTF_8));
	}

	private JwtSubject subject() {
		return new JwtSubject(1L, "ROLE_MEMBER");
	}

}
