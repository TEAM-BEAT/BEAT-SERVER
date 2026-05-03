package com.beat.apis.user.application.result;

public record UserAuthenticationResult(
	Long userId,
	String roleName
) {

	public static UserAuthenticationResult of(Long userId, String roleName) {
		return new UserAuthenticationResult(userId, roleName);
	}
}
