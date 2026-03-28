package com.beat.apis.member.application.dto.response;

public record AccessTokenGenerateResponse(
	String accessToken
) {
	public static AccessTokenGenerateResponse from(
		final String accessToken
	) {
		return new AccessTokenGenerateResponse(accessToken);
	}
}
