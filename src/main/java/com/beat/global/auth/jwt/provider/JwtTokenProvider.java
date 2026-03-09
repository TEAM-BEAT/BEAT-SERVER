package com.beat.global.auth.jwt.provider;

import com.beat.domain.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.access-token-expire-time}")
	private long accessTokenExpireTime;

	@Value("${jwt.refresh-token-expire-time}")
	private long refreshTokenExpireTime;

	private static final String MEMBER_ID = "memberId";
	private static final String ROLE_KEY = "role";

	@PostConstruct
	protected void init() {
		jwtSecret = Base64.getEncoder().encodeToString(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String issueAccessToken(final Authentication authentication) {
		return issueToken(authentication, accessTokenExpireTime);
	}

	public String issueRefreshToken(final Authentication authentication) {
		return issueToken(authentication, refreshTokenExpireTime);
	}

	public JwtValidationType validateToken(String token) {
		try {
			getBody(token);
			return JwtValidationType.VALID_JWT;
		} catch (MalformedJwtException ex) {
			log.error("Invalid JWT Token: {}", ex.getMessage());
			return JwtValidationType.INVALID_JWT_TOKEN;
		} catch (ExpiredJwtException ex) {
			log.error("Expired JWT Token: {}", ex.getMessage());
			return JwtValidationType.EXPIRED_JWT_TOKEN;
		} catch (UnsupportedJwtException ex) {
			log.error("Unsupported JWT Token: {}", ex.getMessage());
			return JwtValidationType.UNSUPPORTED_JWT_TOKEN;
		} catch (IllegalArgumentException ex) {
			log.error("Empty JWT Token or Illegal Argument: {}", ex.getMessage());
			return JwtValidationType.EMPTY_JWT;
		} catch (SignatureException ex) {
			log.error("Invalid JWT Signature: {}", ex.getMessage());
			return JwtValidationType.INVALID_JWT_SIGNATURE;
		}
	}

	public Long getMemberIdFromJwt(String token) {
		Claims claims = getBody(token);
		Long memberId = Long.valueOf(claims.get(MEMBER_ID).toString());
		log.debug("Extracted memberId from JWT: {}", memberId);
		return memberId;
	}

	public Role getRoleFromJwt(String token) {
		Claims claims = getBody(token);
		String roleName = claims.get(ROLE_KEY, String.class);

		log.info("Extracted role from JWT: {}", roleName);

		String enumValue = roleName.replace("ROLE_", "");
		log.info("Final role after processing: {}", enumValue);

		return Role.valueOf(enumValue.toUpperCase());
	}

	private String issueToken(final Authentication authentication, final long expiredTime) {
		final Date now = new Date();
		final Date expiration = new Date(now.getTime() + expiredTime);

		final String memberId = authentication.getPrincipal().toString();
		log.debug("Added member ID to claims: {}", memberId);
		log.info("Authorities before token generation: {}", authentication.getAuthorities());

		String role = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No authorities found for user"));

		log.info("Selected role for token: {}", role);

		return Jwts.builder()
			.header()
			.add(Header.TYPE, Header.JWT_TYPE)
			.and()
			.issuedAt(now)
			.expiration(expiration)
			.claim(MEMBER_ID, memberId)
			.claim(ROLE_KEY, role)
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
		byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}