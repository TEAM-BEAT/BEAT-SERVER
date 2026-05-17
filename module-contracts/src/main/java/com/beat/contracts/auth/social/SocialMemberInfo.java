package com.beat.contracts.auth.social;

public record SocialMemberInfo(
	Long socialId,
	String nickname,
	String email
) {
}
