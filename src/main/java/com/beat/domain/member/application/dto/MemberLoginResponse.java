package com.beat.domain.member.application.dto;

public record MemberLoginResponse(
	String accessToken,
	String nickname,
	String role
) {
	public static MemberLoginResponse of(
		final String accessToken,
		final String nickname,
		final String role
	) {
		return new MemberLoginResponse(accessToken, nickname, role);
	}
}
