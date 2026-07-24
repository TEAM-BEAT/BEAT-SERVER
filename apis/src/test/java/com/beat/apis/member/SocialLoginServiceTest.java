package com.beat.apis.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.beat.apis.member.application.command.LoginTokenIssuer;
import com.beat.apis.member.application.command.SocialLoginMemberResolver;
import com.beat.apis.member.application.command.SocialLoginCommandService;
import com.beat.apis.member.application.command.SocialLoginCommand;
import com.beat.apis.member.application.command.SocialLoginProvider;
import com.beat.apis.member.application.result.LoginSuccessResult;
import com.beat.apis.member.exception.MemberApplicationErrorCode;
import com.beat.apis.member.application.result.MemberAuthenticationResult;
import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialLoginRequest;
import com.beat.contracts.auth.social.SocialLoginType;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.user.model.Role;
import com.beat.domain.user.model.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.apis.exception.ApiApplicationException;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

	@Mock
	private SocialLoginMemberResolver socialLoginMemberResolver;

	@Mock
	private LoginTokenIssuer loginTokenIssuer;

	@Mock
	private SocialLoginPort socialLoginPort;

	@Mock
	private UserRepository userRepository;

	private SocialLoginCommandService socialLoginService;

	@BeforeEach
	void setUp() {
		socialLoginService = new SocialLoginCommandService(
			socialLoginPort,
			socialLoginMemberResolver,
			loginTokenIssuer,
			userRepository
		);
	}

	@Test
	void handleSocialLoginTranslatesUnsupportedSocialTypeFailureToMemberApplicationCode() {
		when(socialLoginPort.login(any()))
			.thenThrow(SocialLoginFailure.unsupportedSocialType());

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () -> socialLoginService.handleSocialLogin("code", SocialLoginCommand.from(SocialLoginProvider.KAKAO)));

		assertEquals(MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST, exception.getErrorCode());
	}

	@Test
	void handleSocialLoginTranslatesAuthenticationFailureToMemberApplicationCode() {
		SocialLoginFailure failure = SocialLoginFailure.authenticationFailed();
		when(socialLoginPort.login(any()))
			.thenThrow(failure);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () -> socialLoginService.handleSocialLogin("code", SocialLoginCommand.from(SocialLoginProvider.KAKAO)));

		assertEquals(MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED, exception.getErrorCode());
		assertSame(failure, exception.getCause());
	}

	@Test
	void handleSocialLoginTranslatesMalformedProviderResponseToBadGateway() {
		RuntimeException providerCause = new RuntimeException("malformed response");
		SocialLoginFailure failure = SocialLoginFailure.providerFailure(providerCause);
		when(socialLoginPort.login(any()))
			.thenThrow(failure);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class,
			() -> socialLoginService.handleSocialLogin("code", SocialLoginCommand.from(SocialLoginProvider.KAKAO)));

		assertEquals(MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_FAILURE, exception.getErrorCode());
		assertSame(failure, exception.getCause());
		assertSame(providerCause, exception.getCause().getCause());
	}

	@Test
	void handleSocialLoginTranslatesProviderAvailabilityFailureToServiceUnavailable() {
		SocialLoginFailure failure = SocialLoginFailure.providerUnavailable(new RuntimeException("unavailable"));
		when(socialLoginPort.login(any()))
			.thenThrow(failure);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class,
			() -> socialLoginService.handleSocialLogin("code", SocialLoginCommand.from(SocialLoginProvider.KAKAO)));

		assertEquals(MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_UNAVAILABLE, exception.getErrorCode());
		assertSame(failure, exception.getCause());
	}

	@Test
	void handleSocialLoginTranslatesProviderTimeoutToGatewayTimeout() {
		SocialLoginFailure failure = SocialLoginFailure.providerTimeout(new RuntimeException("timeout"));
		when(socialLoginPort.login(any()))
			.thenThrow(failure);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class,
			() -> socialLoginService.handleSocialLogin("code", SocialLoginCommand.from(SocialLoginProvider.KAKAO)));

		assertEquals(MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_TIMEOUT, exception.getErrorCode());
		assertSame(failure, exception.getCause());
	}

	@Test
	void handleSocialLoginKeepsRequestedSocialTypeAcrossContractBoundaryWhenRegisteringNewMember() {
		SocialMemberInfo socialMemberInfo = new SocialMemberInfo(123L, "nickname", "email@test.com");
		SocialIdentity socialIdentity = SocialIdentity.of(SocialType.KAKAO, 123L);
		MemberAuthenticationResult member = new MemberAuthenticationResult(1L, 2L);
		Users user = Users.rehydrate(2L, Role.MEMBER);
		LoginSuccessResult expectedResponse = new LoginSuccessResult("access", "refresh", "nickname", "ROLE_MEMBER");

		when(socialLoginPort.login(any())).thenReturn(socialMemberInfo);
		when(socialLoginMemberResolver.findOrRegister(socialMemberInfo, socialIdentity)).thenReturn(member);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(loginTokenIssuer.issue(1L, Role.MEMBER, socialMemberInfo))
			.thenReturn(expectedResponse);

		LoginSuccessResult actual = socialLoginService.handleSocialLogin(
			"authorization-code",
			SocialLoginCommand.from(SocialLoginProvider.KAKAO)
		);

		ArgumentCaptor<SocialLoginRequest> requestCaptor = ArgumentCaptor.forClass(SocialLoginRequest.class);
		verify(socialLoginPort).login(requestCaptor.capture());
		assertEquals("authorization-code", requestCaptor.getValue().getAuthorizationCode());
		assertEquals(SocialLoginType.KAKAO, requestCaptor.getValue().getSocialType());
		verify(socialLoginMemberResolver).findOrRegister(socialMemberInfo, socialIdentity);
		assertEquals(expectedResponse, actual);
	}

	@Test
	void handleSocialLoginReloadsConcurrentRegistrationWinnerAfterUniqueConstraintConflict() {
		SocialMemberInfo socialMemberInfo = new SocialMemberInfo(123L, "nickname", "email@test.com");
		SocialIdentity socialIdentity = SocialIdentity.of(SocialType.KAKAO, 123L);
		MemberAuthenticationResult winner = new MemberAuthenticationResult(1L, 2L);
		Users user = Users.rehydrate(2L, Role.MEMBER);
		LoginSuccessResult expectedResponse = new LoginSuccessResult("access", "refresh", "nickname", "ROLE_MEMBER");

		when(socialLoginPort.login(any())).thenReturn(socialMemberInfo);
		when(socialLoginMemberResolver.findOrRegister(socialMemberInfo, socialIdentity)).thenReturn(winner);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(loginTokenIssuer.issue(1L, Role.MEMBER, socialMemberInfo))
			.thenReturn(expectedResponse);

		LoginSuccessResult actual = socialLoginService.handleSocialLogin(
			"authorization-code",
			SocialLoginCommand.from(SocialLoginProvider.KAKAO)
		);

		assertEquals(expectedResponse, actual);
		verify(socialLoginMemberResolver).findOrRegister(socialMemberInfo, socialIdentity);
	}
}
