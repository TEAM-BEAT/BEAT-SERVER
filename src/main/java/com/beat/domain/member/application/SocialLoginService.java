package com.beat.domain.member.application;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.dto.LoginSuccessResponse;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.user.domain.Users;
import com.beat.global.auth.client.application.KakaoSocialService;
import com.beat.global.auth.client.application.SocialService;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.common.exception.BadRequestException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SocialLoginService {

	private final MemberService memberService;
	private final MemberRegistrationService memberRegistrationService;
	private final AuthenticationService authenticationService;
	private final KakaoSocialService kakaoSocialService;

	@PersistenceContext
	private final EntityManager entityManager;

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
		MemberInfoResponse memberInfoResponse = findMemberInfoFromSocialService(authorizationCode, loginRequest);
		return generateLoginResponseFromMemberInfo(memberInfoResponse);
	}

	/**
	 * 소셜 서비스에서 사용자 정보를 조회하는 메서드.
	 * 소셜 타입에 따라 적절한 소셜 서비스를 사용하여 로그인 정보를 가져옴.
	 *
	 * @param authorizationCode 소셜 인증 코드
	 * @param loginRequest 로그인 요청 정보
	 * @return 소셜 서비스에서 가져온 사용자 정보(MemberInfoResponse)
	 */
	private MemberInfoResponse findMemberInfoFromSocialService(final String authorizationCode,
		final MemberLoginRequest loginRequest) {
		SocialService socialService = findSocialService(loginRequest.socialType());
		return socialService.login(authorizationCode, loginRequest);
	}

	/**
	 * 소셜 타입에 맞는 SocialService를 반환하는 메서드.
	 * 소셜 로그인 타입이 KAKAO인지, GOOGLE인지 등에 따라 적절한 서비스를 반환.
	 *
	 * @param socialType 소셜 타입(KAKAO, GOOGLE 등)
	 * @return 적절한 SocialService 구현체
	 */
	private SocialService findSocialService(SocialType socialType) {
		return switch (socialType) {
			case KAKAO -> kakaoSocialService;
			// case GOOGLE -> googleSocialService;
			default -> throw new BadRequestException(MemberErrorCode.SOCIAL_TYPE_BAD_REQUEST);
		};
	}

	/**
	 * 사용자 정보를 기반으로 로그인 또는 회원가입을 처리한 후 로그인 성공 응답을 생성하는 메서드.
	 * 사용자가 존재하면 로그인 처리를, 존재하지 않으면 회원가입 후 로그인 처리를 수행.
	 *
	 * @param memberInfoResponse 소셜 서비스에서 가져온 사용자 정보
	 * @return 로그인 성공 응답(LoginSuccessResponse)
	 */
	private LoginSuccessResponse generateLoginResponseFromMemberInfo(final MemberInfoResponse memberInfoResponse) {
		Long memberId = findOrRegisterMember(memberInfoResponse);
		entityManager.flush();

		Users user = memberService.findUserByMemberId(memberId);

		return authenticationService.generateLoginSuccessResponse(memberId, user, memberInfoResponse);
	}

	/**
	 * 사용자 정보(Social ID와 Social Type)를 통해 기존 회원을 찾거나,
	 * 없으면 새로운 회원을 등록하는 메서드.
	 *
	 * @param memberInfoResponse 소셜 서비스에서 가져온 사용자 정보
	 * @return 등록된 회원 또는 기존 회원의 ID
	 */
	private Long findOrRegisterMember(final MemberInfoResponse memberInfoResponse) {
		boolean memberExists = memberService.checkMemberExistsBySocialIdAndSocialType(memberInfoResponse.socialId(),
			memberInfoResponse.socialType());

		if (memberExists) {
			Member existingMember = memberService.findMemberBySocialIdAndSocialType(memberInfoResponse.socialId(),
				memberInfoResponse.socialType());
			return existingMember.getId();
		}

		return memberRegistrationService.registerMemberWithUserInfo(memberInfoResponse);
	}
}