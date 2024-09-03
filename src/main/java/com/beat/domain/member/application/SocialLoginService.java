package com.beat.domain.member.application;

import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.dto.LoginSuccessResponse;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.global.auth.client.application.KakaoSocialService;
import com.beat.global.auth.client.application.SocialService;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.common.exception.BadRequestException;
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

    @Transactional
    public LoginSuccessResponse handleSocialLogin(final String authorizationCode, final MemberLoginRequest loginRequest) {
        MemberInfoResponse memberInfoResponse = findMemberInfoFromSocialService(authorizationCode, loginRequest);
        return generateLoginResponseFromMemberInfo(memberInfoResponse);
    }

    public MemberInfoResponse findMemberInfoFromSocialService(final String authorizationCode, final MemberLoginRequest loginRequest) {
        SocialService socialService = findSocialService(loginRequest.socialType());
        return socialService.login(authorizationCode, loginRequest);
    }

    private SocialService findSocialService(SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> kakaoSocialService;
            // case GOOGLE -> googleSocialService;
            default -> throw new BadRequestException(MemberErrorCode.SOCIAL_TYPE_BAD_REQUEST);
        };
    }

    private LoginSuccessResponse generateLoginResponseFromMemberInfo(final MemberInfoResponse memberInfoResponse) {
        Long memberId = findOrRegisterMember(memberInfoResponse);
        return authenticationService.generateLoginSuccessResponse(memberId, memberInfoResponse);
    }

    private Long findOrRegisterMember(final MemberInfoResponse memberInfoResponse) {
        boolean memberExists = memberService.checkMemberExistsBySocialIdAndSocialType(memberInfoResponse.socialId(), memberInfoResponse.socialType());

        if (memberExists) {
            Member existingMember = memberService.findMemberBySocialIdAndSocialType(memberInfoResponse.socialId(), memberInfoResponse.socialType());
            return existingMember.getId();
        }

        return memberRegistrationService.registerMemberWithUserInfo(memberInfoResponse);
    }
}