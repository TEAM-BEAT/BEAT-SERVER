package com.beat.gateway.jwt.internal;

import com.beat.contracts.auth.JwtSubject;
import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.JwtTokenType;
import com.beat.contracts.auth.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtTokenProvider implements JwtTokenPort {

	private static final String MEMBER_ID = "memberId";
	private static final String ROLE_KEY = "role";
	private static final String TOKEN_TYPE = "tokenType";
	private static final long LEGACY_ACCESS_TOKEN_CLOCK_SKEW_MILLIS = Duration.ofMinutes(10).toMillis();

	private final JwtProperties jwtProperties;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	@PostConstruct
	protected void validateSigningKeyConfiguration() {
		validateRequiredConfiguration();
		getCurrentSigningKey();
		if (isBeforeLegacyCutoff()) {
			getLegacySigningKey();
		}
	}

	@Override
	public String issueAccessToken(JwtSubject subject) {
		return issueToken(subject, jwtProperties.accessTokenExpireTime(), JwtTokenType.ACCESS);
	}

	@Override
	public String issueRefreshToken(JwtSubject subject) {
		return issueToken(subject, jwtProperties.refreshTokenExpireTime(), JwtTokenType.REFRESH);
	}

	@Override
	public TokenValidationResult validateAccessToken(String token) {
		return validateToken(token, JwtTokenType.ACCESS);
	}

	@Override
	public TokenValidationResult validateRefreshToken(String token) {
		return validateToken(token, JwtTokenType.REFRESH);
	}

	@Override
	public Long getMemberId(String token, JwtTokenType expectedType) {
		Claims claims = getBody(token, expectedType);
		Object memberIdClaim = claims.get(MEMBER_ID);

		if (memberIdClaim == null) {
			throw new IllegalArgumentException("JWT does not contain memberId claim");
		}

		return Long.valueOf(memberIdClaim.toString());
	}

	@Override
	public String getRoleName(String token, JwtTokenType expectedType) {
		Claims claims = getBody(token, expectedType);
		String roleName = claims.get(ROLE_KEY, String.class);

		if (roleName == null || roleName.isBlank()) {
			throw new IllegalArgumentException("JWT does not contain role claim");
		}

		return roleName;
	}

	private TokenValidationResult validateToken(String token, JwtTokenType expectedType) {
		try {
			Claims claims = getBody(token, expectedType);

			if (!hasValidMemberId(claims)) {
				log.warn("JWT does not contain memberId claim");
				return TokenValidationResult.INVALID_TOKEN;
			}

			String roleName = claims.get(ROLE_KEY, String.class);
			if (roleName == null || roleName.isBlank()) {
				log.warn("JWT does not contain role claim");
				return TokenValidationResult.INVALID_TOKEN;
			}

			return TokenValidationResult.VALID;
		} catch (MalformedJwtException ex) {
			log.error("Invalid JWT Token: {}", ex.getMessage());
			return TokenValidationResult.INVALID_TOKEN;
		} catch (ExpiredJwtException ex) {
			log.error("Expired JWT Token: {}", ex.getMessage());
			return TokenValidationResult.EXPIRED;
		} catch (UnsupportedJwtException ex) {
			log.error("Unsupported JWT Token: {}", ex.getMessage());
			return TokenValidationResult.UNSUPPORTED;
		} catch (InvalidTokenClaimsException ex) {
			log.error("Invalid JWT claims: {}", ex.getMessage());
			return TokenValidationResult.INVALID_TOKEN;
		} catch (IllegalArgumentException ex) {
			log.error("Empty JWT Token or Illegal Argument: {}", ex.getMessage());
			return TokenValidationResult.EMPTY;
		} catch (SignatureException ex) {
			log.error("Invalid JWT Signature: {}", ex.getMessage());
			return TokenValidationResult.INVALID_SIGNATURE;
		}
	}

	private String issueToken(final JwtSubject subject, final long expiredTime, final JwtTokenType tokenType) {
		final Instant now = Instant.now();
		final Instant expiration = now.plusMillis(expiredTime);

		return Jwts.builder()
			.header()
			.keyId(jwtProperties.keyId())
			.and()
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiration))
			.claim(MEMBER_ID, subject.memberId())
			.claim(ROLE_KEY, subject.roleName())
			.claim(TOKEN_TYPE, tokenType.name())
			.signWith(getCurrentSigningKey())
			.compact();
	}

	private Claims getBody(final String token, final JwtTokenType expectedType) {
		try {
			Claims claims = parseWithCurrentKey(token);
			validateCurrentTokenType(claims, expectedType);
			log.debug("JWT validated with current key for expected tokenType={}", expectedType);
			return claims;
		} catch (SignatureException currentSignatureFailure) {
			if (expectedType == JwtTokenType.ACCESS && isBeforeLegacyCutoff()) {
				Claims claims = parseWithLegacyKey(token);
				validateLegacyAccessWindow(claims);
				log.warn("JWT legacy access fallback succeeded before cutoff={}",
					jwtProperties.legacyAccessTokenVerifyUntil());
				return claims;
			}
			log.warn("JWT legacy access fallback rejected for expected tokenType={} cutoff={}", expectedType,
				jwtProperties.legacyAccessTokenVerifyUntil());
			throw currentSignatureFailure;
		}
	}

	private Claims parseWithCurrentKey(final String token) {
		return Jwts.parser()
			.verifyWith(getCurrentSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private Claims parseWithLegacyKey(final String token) {
		return Jwts.parser()
			.verifyWith(getLegacySigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private void validateCurrentTokenType(final Claims claims, final JwtTokenType expectedType) {
		String actualType = claims.get(TOKEN_TYPE, String.class);
		if (!expectedType.name().equals(actualType)) {
			log.warn("JWT tokenType mismatch: expected={}, actual={}", expectedType, actualType);
			throw new InvalidTokenClaimsException("JWT tokenType does not match expected type");
		}
	}

	private void validateLegacyAccessWindow(final Claims claims) {
		Date issuedAt = claims.getIssuedAt();
		Date expiration = claims.getExpiration();
		if (issuedAt == null || expiration == null) {
			log.warn("JWT legacy fallback rejected because iat or exp is missing");
			throw new InvalidTokenClaimsException("Legacy JWT is missing iat or exp");
		}

		long lifetimeMillis = expiration.getTime() - issuedAt.getTime();
		long maximumLegacyAccessLifetime = jwtProperties.accessTokenExpireTime()
			+ LEGACY_ACCESS_TOKEN_CLOCK_SKEW_MILLIS;
		if (lifetimeMillis > maximumLegacyAccessLifetime) {
			log.warn("JWT legacy fallback rejected by lifetime heuristic: lifetimeMillis={}, maximumMillis={}",
				lifetimeMillis, maximumLegacyAccessLifetime);
			throw new InvalidTokenClaimsException("Legacy JWT lifetime exceeds access token window");
		}
	}

	private boolean isBeforeLegacyCutoff() {
		return Instant.now().isBefore(jwtProperties.legacyAccessTokenVerifyUntil());
	}

	private boolean hasValidMemberId(final Claims claims) {
		Object memberIdClaim = claims.get(MEMBER_ID);
		if (memberIdClaim == null) {
			return false;
		}

		try {
			Long.valueOf(memberIdClaim.toString());
			return true;
		} catch (NumberFormatException _) {
			log.warn("JWT memberId claim is not numeric: {}", memberIdClaim);
			return false;
		}
	}

	private void validateRequiredConfiguration() {
		requireText(jwtProperties.secret(), "jwt.secret");
		requireText(jwtProperties.keyId(), "jwt.key-id");
		if (jwtProperties.legacyAccessTokenVerifyUntil() == null) {
			throw new IllegalStateException("jwt.legacy-access-token-verify-until is required");
		}
		requirePositive(jwtProperties.accessTokenExpireTime(), "jwt.access-token-expire-time");
		requirePositive(jwtProperties.refreshTokenExpireTime(), "jwt.refresh-token-expire-time");
	}

	private void requireText(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(propertyName + " is required");
		}
	}

	private void requirePositive(long value, String propertyName) {
		if (value <= 0) {
			throw new IllegalStateException(propertyName + " must be positive");
		}
	}

	private SecretKey getCurrentSigningKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private SecretKey getLegacySigningKey() {
		requireLegacySecretConfigured();

		String onceEncoded = Base64.getEncoder()
			.encodeToString(jwtProperties.legacySecret().getBytes(StandardCharsets.UTF_8));
		String twiceEncoded = Base64.getEncoder()
			.encodeToString(onceEncoded.getBytes(StandardCharsets.UTF_8));
		return Keys.hmacShaKeyFor(twiceEncoded.getBytes(StandardCharsets.UTF_8));
	}

	private void requireLegacySecretConfigured() {
		if (jwtProperties.legacySecret() == null || jwtProperties.legacySecret().isBlank()) {
			throw new IllegalStateException("jwt.legacy-secret is required for legacy JWT verification");
		}
	}

	private static class InvalidTokenClaimsException extends IllegalArgumentException {

		InvalidTokenClaimsException(String message) {
			super(message);
		}
	}
}
