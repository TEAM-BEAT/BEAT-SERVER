package com.beat.domain.member.dto;

public record LoginSuccessResponse(
	String accessToken,
	String refreshToken,
	String nickname,
	String role
) {
	public static LoginSuccessResponse of(
		final String accessToken,
		final String refreshToken,
		final String nickname,
		final String role
	) {
		return new LoginSuccessResponse(accessToken, refreshToken, nickname, role);
	}
}
