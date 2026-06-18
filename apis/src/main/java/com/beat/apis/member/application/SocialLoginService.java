package com.beat.apis.member.application;

import com.beat.apis.common.application.converter.SocialTypeEnumConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.member.application.dto.request.MemberLoginRequest;
import com.beat.apis.member.application.dto.response.LoginSuccessResponse;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.member.application.result.MemberAuthenticationResult;
import com.beat.apis.user.application.UserService;
import com.beat.apis.user.application.result.UserAuthenticationResult;
import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialLoginRequest;
import com.beat.contracts.auth.social.SocialLoginType;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.member.domain.SocialType;
import com.beat.global.support.exception.BadRequestException;
import com.beat.global.support.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class SocialLoginService {

	private final MemberRegistrationService memberRegistrationService;
	private final AuthenticationService authenticationService;
	private final SocialLoginPort socialLoginPort;
	private final MemberService memberService;
	private final UserService userService;

	/**
	 * 소셜 로그인 또는 회원가입을 처리하는 메서드.
	 * 소셜 서비스에서 받은 authorizationCode와 로그인 요청 정보를 기반으로
	 * 사용자 정보를 조회하고, 로그인 또는 회원가입 후 성공 응답을 반환.
	 *
	 * @param authorizationCode 소셜 인증 코드
	 * @param loginRequest 로그인 요청 정보
	 * @return 로그인 성공 응답(LoginSuccessResponse)
	 */
	@Transactional
	public LoginSuccessResponse handleSocialLogin(final String authorizationCode,
		final MemberLoginRequest loginRequest) {
		SocialType socialType = SocialTypeEnumConverter.toDomain(loginRequest.socialType());
		SocialLoginRequest request = new SocialLoginRequest(authorizationCode, toContractSocialType(socialType));
		try {
			SocialMemberInfo socialMemberInfo = socialLoginPort.login(request);
			return generateLoginResponseFromMemberInfo(socialMemberInfo, socialType);
		} catch (SocialLoginFailure failure) {
			throw translateSocialLoginFailure(failure);
		}
	}

	/**
	 * 사용자 정보를 기반으로 로그인 또는 회원가입을 처리한 후 로그인 성공 응답을 생성하는 메서드.
	 * 사용자가 존재하면 로그인 처리를, 존재하지 않으면 회원가입 후 로그인 처리를 수행.
	 *
	 * @param socialMemberInfo 소셜 서비스에서 가져온 사용자 정보
	 * @param socialType 요청된 소셜 로그인 타입
	 * @return 로그인 성공 응답(LoginSuccessResponse)
	 */
	private RuntimeException translateSocialLoginFailure(SocialLoginFailure failure) {
		return switch (failure.getReason()) {
			case UNSUPPORTED_SOCIAL_TYPE -> new BadRequestException(MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST);
			case AUTHENTICATION_FAILED ->
				new UnauthorizedException(MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED);
		};
	}

	private LoginSuccessResponse generateLoginResponseFromMemberInfo(final SocialMemberInfo socialMemberInfo,
		final SocialType socialType) {
		log.info("Attempting to find or register member for socialId: {}, socialType: {}",
			socialMemberInfo.getSocialId(), socialType);

		Long memberId = findOrRegisterMember(socialMemberInfo, socialType);
		log.info("Found or registered member with memberId: {}", memberId);

		MemberAuthenticationResult member = memberService.findMemberAuthenticationResultByMemberId(memberId);
		UserAuthenticationResult user = findUserAuthenticationResult(member.userId());

		log.info("User role before generating token: {}", user.roleName());

		return authenticationService.generateLoginSuccessResponse(memberId, user.roleName(), socialMemberInfo);
	}

	/**
	 * 사용자 정보(Social ID와 Social Type)를 통해 기존 회원을 찾거나,
	 * 없으면 새로운 회원을 등록하는 메서드.
	 *
	 * @param socialMemberInfo 소셜 서비스에서 가져온 사용자 정보
	 * @param socialType 요청된 소셜 로그인 타입
	 * @return 등록된 회원 또는 기존 회원의 ID
	 */
	private Long findOrRegisterMember(final SocialMemberInfo socialMemberInfo, final SocialType socialType) {
		boolean memberExists = memberService.checkMemberExistsBySocialIdAndSocialType(
			socialMemberInfo.getSocialId(),
			socialType
		);

		if (memberExists) {
			MemberAuthenticationResult existingMember =
				memberService.findMemberAuthenticationResultBySocialIdAndSocialType(
					socialMemberInfo.getSocialId(),
					socialType
				);
			UserAuthenticationResult user = findUserAuthenticationResult(existingMember.userId());
			log.info("Existing member role: {}", user.roleName());
			return existingMember.memberId();
		}

		return memberRegistrationService.registerMemberWithUserInfo(socialMemberInfo, socialType);
	}

	private UserAuthenticationResult findUserAuthenticationResult(Long userId) {
		return userService.findUserAuthenticationByUserId(userId);
	}

	private SocialLoginType toContractSocialType(SocialType socialType) {
		return switch (socialType) {
			case KAKAO -> SocialLoginType.KAKAO;
		};
	}
}
