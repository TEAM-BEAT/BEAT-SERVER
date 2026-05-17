package com.beat.contracts.auth.social;

public record SocialLoginRequest(
	String authorizationCode,
	SocialLoginType socialType
) {
}
