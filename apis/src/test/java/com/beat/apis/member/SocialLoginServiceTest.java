package com.beat.apis.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.member.application.AuthenticationService;
import com.beat.apis.member.application.MemberRegistrationService;
import com.beat.apis.member.application.MemberService;
import com.beat.apis.member.application.SocialLoginService;
import com.beat.apis.member.application.dto.request.MemberLoginRequest;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

	@Mock
	private MemberRegistrationService memberRegistrationService;

	@Mock
	private AuthenticationService authenticationService;

	@Mock
	private SocialLoginPort socialLoginPort;

	@Mock
	private MemberService memberService;

	@Mock
	private UserRepository userRepository;

	private SocialLoginService socialLoginService;

	@BeforeEach
	void setUp() {
		socialLoginService = new SocialLoginService(
			memberRegistrationService,
			authenticationService,
			socialLoginPort,
			memberService,
			userRepository
		);
	}

	@Test
	void handleSocialLoginTranslatesUnsupportedSocialTypeFailureToMemberApplicationCode() {
		when(socialLoginPort.login(any()))
			.thenThrow(SocialLoginFailure.unsupportedSocialType());

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> socialLoginService.handleSocialLogin("code", new MemberLoginRequest(SocialType.KAKAO))
		);

		assertEquals(MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST, exception.getBaseErrorCode());
	}

	@Test
	void handleSocialLoginTranslatesAuthenticationFailureToMemberApplicationCode() {
		when(socialLoginPort.login(any()))
			.thenThrow(SocialLoginFailure.authenticationFailed());

		UnauthorizedException exception = assertThrows(
			UnauthorizedException.class,
			() -> socialLoginService.handleSocialLogin("code", new MemberLoginRequest(SocialType.KAKAO))
		);

		assertEquals(MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED, exception.getBaseErrorCode());
	}
}
