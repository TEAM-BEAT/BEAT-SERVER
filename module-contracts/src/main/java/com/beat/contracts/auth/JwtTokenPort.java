package com.beat.contracts.auth;

public interface JwtTokenPort {

	String issueAccessToken(JwtSubject subject);

	String issueRefreshToken(JwtSubject subject);

	TokenValidationResult validateToken(String token);

	Long getMemberId(String token);

	String getRoleName(String token);
}
