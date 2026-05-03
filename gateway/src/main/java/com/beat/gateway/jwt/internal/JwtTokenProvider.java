package com.beat.gateway.jwt.internal;

import com.beat.contracts.auth.JwtSubject;
import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenProvider implements JwtTokenPort {

	private static final String MEMBER_ID = "memberId";
	private static final String ROLE_KEY = "role";

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.access-token-expire-time}")
	private long accessTokenExpireTime;

	@Value("${jwt.refresh-token-expire-time}")
	private long refreshTokenExpireTime;

	@PostConstruct
	protected void init() {
		jwtSecret = Base64.getEncoder().encodeToString(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String issueAccessToken(JwtSubject subject) {
		return issueToken(subject, accessTokenExpireTime);
	}

	@Override
	public String issueRefreshToken(JwtSubject subject) {
		return issueToken(subject, refreshTokenExpireTime);
	}

	@Override
	public TokenValidationResult validateToken(String token) {
		try {
			Claims claims = getBody(token);

			if (claims.get(MEMBER_ID) == null) {
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
		} catch (IllegalArgumentException ex) {
			log.error("Empty JWT Token or Illegal Argument: {}", ex.getMessage());
			return TokenValidationResult.EMPTY;
		} catch (SignatureException ex) {
			log.error("Invalid JWT Signature: {}", ex.getMessage());
			return TokenValidationResult.INVALID_SIGNATURE;
		}
	}

	@Override
	public Long getMemberId(String token) {
		Claims claims = getBody(token);
		Object memberIdClaim = claims.get(MEMBER_ID);

		if (memberIdClaim == null) {
			throw new IllegalArgumentException("JWT does not contain memberId claim");
		}

		return Long.valueOf(memberIdClaim.toString());
	}

	@Override
	public String getRoleName(String token) {
		Claims claims = getBody(token);
		String roleName = claims.get(ROLE_KEY, String.class);

		if (roleName == null || roleName.isBlank()) {
			throw new IllegalArgumentException("JWT does not contain role claim");
		}

		return roleName;
	}

	private String issueToken(final JwtSubject subject, final long expiredTime) {
		final Date now = new Date();
		final Date expiration = new Date(now.getTime() + expiredTime);

		return Jwts.builder()
			.issuedAt(now)
			.expiration(expiration)
			.claim(MEMBER_ID, subject.memberId())
			.claim(ROLE_KEY, subject.roleName())
			.signWith(getSigningKey())
			.compact();
	}

	private Claims getBody(final String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private SecretKey getSigningKey() {
		String encodedKey = Base64.getEncoder().encodeToString(jwtSecret.getBytes());
		return Keys.hmacShaKeyFor(encodedKey.getBytes());
	}
}
