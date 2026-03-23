package com.beat.contracts.auth.social;

import com.beat.domain.member.domain.SocialType;

public record SocialMemberInfo(
	Long socialId,
	String nickname,
	String email,
	SocialType socialType
) {
}
