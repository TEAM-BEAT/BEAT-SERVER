package com.beat.contracts.auth.social;

import com.beat.domain.member.domain.SocialType;

public record SocialLoginCommand(
	String authorizationCode,
	SocialType socialType
) {
}
