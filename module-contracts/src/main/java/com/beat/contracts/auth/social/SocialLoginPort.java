package com.beat.contracts.auth.social;

public interface SocialLoginPort {

	SocialMemberInfo login(SocialLoginRequest request);
}
