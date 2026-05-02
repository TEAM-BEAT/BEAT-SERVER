package com.beat.apis.member.application.result;

public record MemberAuthenticationResult(
	Long memberId,
	Long userId
) {

	public static MemberAuthenticationResult of(Long memberId, Long userId) {
		return new MemberAuthenticationResult(memberId, userId);
	}
}
