package com.beat.contracts.auth;

public interface JwtTokenPort {

	String issueAccessToken(JwtSubject subject);

	String issueRefreshToken(JwtSubject subject);

	TokenValidationResult validateAccessToken(String token);

	TokenValidationResult validateRefreshToken(String token);

	@Deprecated(forRemoval = true)
	default TokenValidationResult validateToken(String token) {
		return validateAccessToken(token);
	}

	Long getMemberId(String token, JwtTokenType expectedType);

	String getRoleName(String token, JwtTokenType expectedType);

	@Deprecated(forRemoval = true)
	default Long getMemberId(String token) {
		return getMemberId(token, JwtTokenType.ACCESS);
	}

	@Deprecated(forRemoval = true)
	default String getRoleName(String token) {
		return getRoleName(token, JwtTokenType.ACCESS);
	}
}
