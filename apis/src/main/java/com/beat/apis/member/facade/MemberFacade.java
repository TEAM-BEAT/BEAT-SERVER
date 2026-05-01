package com.beat.apis.member.facade;

import org.springframework.stereotype.Service;

import com.beat.apis.member.application.AuthenticationService;
import com.beat.apis.member.application.SocialLoginService;
import com.beat.apis.member.application.dto.request.MemberLoginRequest;
import com.beat.apis.member.application.dto.response.AccessTokenGenerateResponse;
import com.beat.apis.member.application.dto.response.LoginSuccessResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberFacade {
	private final AuthenticationService authenticationService;
	private final SocialLoginService socialLoginService;

	public LoginSuccessResponse handleSocialLogin(String authorizationCode, MemberLoginRequest loginRequest) {
		return socialLoginService.handleSocialLogin(authorizationCode, loginRequest);
	}

	public AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(String refreshToken) {
		return authenticationService.generateAccessTokenFromRefreshToken(refreshToken);
	}

	public void signOut(Long memberId) {
		authenticationService.signOut(memberId);
	}
}
