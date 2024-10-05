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

	private String issueToken(final Authentication authentication, final long expiredTime) {
		final Date now = new Date();

		final Claims claims = Jwts.claims()
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + expiredTime));

		claims.put(MEMBER_ID, authentication.getPrincipal());
		claims.put(ROLE_KEY, authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No authorities found for user")));

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setClaims(claims)
			.signWith(getSigningKey())
			.compact();
	}

	private SecretKey getSigningKey() {
		String encodedKey = Base64.getEncoder().encodeToString(jwtSecret.getBytes());
		return Keys.hmacShaKeyFor(encodedKey.getBytes());
	}

	public JwtValidationType validateToken(String token) {
		try {
			Claims claims = getBody(token);
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

	private Claims getBody(final String token) {
		return Jwts.parserBuilder()
			.setSigningKey(getSigningKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	public Long getMemberIdFromJwt(String token) {
		Claims claims = getBody(token);
		Long memberId = Long.valueOf(claims.get(MEMBER_ID).toString());

		// 로그 추가: memberId 확인
		log.info("Extracted memberId from JWT: {}", memberId);

		return memberId;
	}

	public Role getRoleFromJwt(String token) {
		Claims claims = getBody(token);
		String roleName = claims.get(ROLE_KEY, String.class);

		// "ROLE_" 접두사 제거
		String enumValue = roleName.replace("ROLE_", "");
		log.info("Extracted role from JWT: {}", enumValue);

		return Role.valueOf(enumValue.toUpperCase());
	}
}