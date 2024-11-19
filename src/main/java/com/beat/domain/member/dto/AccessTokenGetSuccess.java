package com.beat.domain.member.dto;

public record AccessTokenGetSuccess(
	String accessToken
) {
	public static AccessTokenGetSuccess of(
		final String accessToken
	) {
		return new AccessTokenGetSuccess(accessToken);
	}
}
