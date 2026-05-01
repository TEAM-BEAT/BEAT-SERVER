package com.beat.apis.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.member.application.AuthenticationService;
import com.beat.apis.member.application.MemberRegistrationService;
import com.beat.apis.member.application.MemberService;
import com.beat.apis.member.application.SocialLoginService;
import com.beat.apis.member.application.dto.request.MemberLoginRequest;
import com.beat.apis.member.application.dto.response.LoginSuccessResponse;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialLoginRequest;
import com.beat.contracts.auth.social.SocialLoginType;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
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

	@Test
	void handleSocialLoginKeepsRequestedSocialTypeAcrossContractBoundaryWhenRegisteringNewMember() {
		SocialMemberInfo socialMemberInfo = new SocialMemberInfo(123L, "nickname", "email@test.com");
		Member member = Member.rehydrate(1L, "nickname", "email@test.com", null, 2L, 123L, SocialType.KAKAO);
		Users user = Users.rehydrate(2L, Role.MEMBER);
		LoginSuccessResponse expectedResponse = LoginSuccessResponse.of("access", "refresh", "nickname", "ROLE_MEMBER");

		when(socialLoginPort.login(any())).thenReturn(socialMemberInfo);
		when(memberService.checkMemberExistsBySocialIdAndSocialType(123L, SocialType.KAKAO)).thenReturn(false);
		when(memberRegistrationService.registerMemberWithUserInfo(socialMemberInfo, SocialType.KAKAO)).thenReturn(1L);
		when(memberService.findMemberByMemberId(1L)).thenReturn(member);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(authenticationService.generateLoginSuccessResponse(1L, Role.MEMBER, socialMemberInfo))
			.thenReturn(expectedResponse);

		LoginSuccessResponse actual = socialLoginService.handleSocialLogin(
			"authorization-code",
			new MemberLoginRequest(SocialType.KAKAO)
		);

		ArgumentCaptor<SocialLoginRequest> requestCaptor = ArgumentCaptor.forClass(SocialLoginRequest.class);
		verify(socialLoginPort).login(requestCaptor.capture());
		assertEquals("authorization-code", requestCaptor.getValue().authorizationCode());
		assertEquals(SocialLoginType.KAKAO, requestCaptor.getValue().socialType());
		verify(memberRegistrationService).registerMemberWithUserInfo(socialMemberInfo, SocialType.KAKAO);
		assertEquals(expectedResponse, actual);
	}
}
