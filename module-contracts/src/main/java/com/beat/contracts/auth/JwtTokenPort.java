package com.beat.contracts.auth;

public interface JwtTokenPort {

	String issueAccessToken(JwtSubject subject);

	String issueRefreshToken(JwtSubject subject);

	TokenValidationResult validateAccessToken(String token);

	TokenValidationResult validateRefreshToken(String token);

	Long getMemberId(String token, JwtTokenType expectedType);

	String getRoleName(String token, JwtTokenType expectedType);
}
