package com.beat.apis.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.member.application.dto.request.MemberLoginRequest;
import com.beat.apis.member.application.dto.response.LoginSuccessResponse;
import com.beat.contracts.auth.social.SocialLoginCommand;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.member.domain.Member;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beat.apis.user.application.exception.UserApplicationErrorCode;

@Slf4j
@RequiredArgsConstructor
@Service
public class SocialLoginService {

	private final MemberRegistrationService memberRegistrationService;
	private final AuthenticationService authenticationService;
	private final SocialLoginPort socialLoginPort;
	private final MemberService memberService;
	private final UserRepository userRepository;

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
		SocialLoginCommand command = new SocialLoginCommand(authorizationCode, loginRequest.socialType());
		SocialMemberInfo socialMemberInfo = socialLoginPort.login(command);
		return generateLoginResponseFromMemberInfo(socialMemberInfo);
	}

	/**
	 * 사용자 정보를 기반으로 로그인 또는 회원가입을 처리한 후 로그인 성공 응답을 생성하는 메서드.
	 * 사용자가 존재하면 로그인 처리를, 존재하지 않으면 회원가입 후 로그인 처리를 수행.
	 *
	 * @param socialMemberInfo 소셜 서비스에서 가져온 사용자 정보
	 * @return 로그인 성공 응답(LoginSuccessResponse)
	 */
	private LoginSuccessResponse generateLoginResponseFromMemberInfo(final SocialMemberInfo socialMemberInfo) {
		log.info("Attempting to find or register member for socialId: {}, socialType: {}",
			socialMemberInfo.socialId(), socialMemberInfo.socialType());

		Long memberId = findOrRegisterMember(socialMemberInfo);
		log.info("Found or registered member with memberId: {}", memberId);

		Member member = memberService.findMemberByMemberId(memberId);
		Users user = userRepository.findById(member.getUserId())
			.orElseThrow(() -> new NotFoundException(UserApplicationErrorCode.USER_NOT_FOUND));

		log.info("User role before generating token: {}", user.getRole());

		return authenticationService.generateLoginSuccessResponse(memberId, user.getRole(), socialMemberInfo);
	}

	/**
	 * 사용자 정보(Social ID와 Social Type)를 통해 기존 회원을 찾거나,
	 * 없으면 새로운 회원을 등록하는 메서드.
	 *
	 * @param socialMemberInfo 소셜 서비스에서 가져온 사용자 정보
	 * @return 등록된 회원 또는 기존 회원의 ID
	 */
	private Long findOrRegisterMember(final SocialMemberInfo socialMemberInfo) {
		boolean memberExists = memberService.checkMemberExistsBySocialIdAndSocialType(socialMemberInfo.socialId(),
			socialMemberInfo.socialType());

		if (memberExists) {
			Member existingMember = memberService.findMemberBySocialIdAndSocialType(socialMemberInfo.socialId(),
				socialMemberInfo.socialType());
			Users user = userRepository.findById(existingMember.getUserId())
				.orElseThrow(() -> new NotFoundException(UserApplicationErrorCode.USER_NOT_FOUND));
			log.info("Existing member role: {}", user.getRole());
			return existingMember.getId();
		}

		return memberRegistrationService.registerMemberWithUserInfo(socialMemberInfo);
	}
}
