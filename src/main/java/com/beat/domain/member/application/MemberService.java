package com.beat.domain.member.application;

import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.dto.*;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.global.auth.client.application.SocialService;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.client.application.KakaoSocialService;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.auth.jwt.exception.TokenErrorCode;
import com.beat.global.auth.jwt.provider.JwtTokenProvider;
import com.beat.global.auth.security.MemberAuthentication;
import com.beat.global.common.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MemberService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final KakaoSocialService kakaoSocialService;

    @Transactional
    public LoginSuccessResponse handleSocialLogin(
            final String authorizationCode,
            final MemberLoginRequest loginRequest
    ) {
        MemberInfoResponse memberInfoResponse = getMemberInfoResponse(authorizationCode, loginRequest);
        return generateLoginResponse(memberInfoResponse);
    }

    public MemberInfoResponse getMemberInfoResponse(
            final String authorizationCode,
            final MemberLoginRequest loginRequest
    ) {
        SocialService socialService = getSocialService(loginRequest.socialType());
        return socialService.login(authorizationCode, loginRequest);
    }

    private SocialService getSocialService(SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> kakaoSocialService;
            // case GOOGLE -> googleSocialService;

            default -> throw new BadRequestException(MemberErrorCode.SOCIAL_TYPE_BAD_REQUEST);
        };
    }

    @Transactional
    public Long createUser(final MemberInfoResponse userResponse) {
        Users users = Users.create();
        users = userRepository.save(users);

        Member member = Member.create(
                userResponse.nickname(),
                userResponse.email(),
                users,
                userResponse.socialId(),
                userResponse.socialType()
        );

        memberRepository.save(member);

        return member.getId();
    }

    @Transactional
    public AccessTokenGetSuccess refreshToken(
            final String refreshToken
    ) {
        Long memberId = jwtTokenProvider.getUserFromJwt(refreshToken);
        if (!memberId.equals(tokenService.findIdByRefreshToken(refreshToken))) {
            throw new BadRequestException(TokenErrorCode.TOKEN_INCORRECT_ERROR);
        }
        MemberAuthentication memberAuthentication = new MemberAuthentication(memberId, null, null);
        return AccessTokenGetSuccess.of(
                jwtTokenProvider.issueAccessToken(memberAuthentication)
        );
    }

    @Transactional
    public void deleteUser(
            final Long id
    ) {
        Users users = userRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND)
                );
        userRepository.delete(users);
    }

    private String issueAndSaveRefreshToken(Long id, MemberAuthentication memberAuthentication) {
        String refreshToken = jwtTokenProvider.issueRefreshToken(memberAuthentication);
        tokenService.saveRefreshToken(id, refreshToken);
        return refreshToken;
    }

    private LoginSuccessResponse generateLoginResponse(
            final MemberInfoResponse memberInfoResponse) {
        Long memberId = findOrRegisterMember(memberInfoResponse);
        return createLoginSuccessResponse(memberId, memberInfoResponse);
    }

    private Long findOrRegisterMember(
            final MemberInfoResponse memberInfoResponse) {
        boolean isExisting = isExistingMember(memberInfoResponse.socialId(), memberInfoResponse.socialType());

        if (isExisting) {
            return getMemberIdBySocialInfo(memberInfoResponse.socialId(), memberInfoResponse.socialType());
        }

        return createUser(memberInfoResponse);
    }

    public boolean isExistingMember(
            final Long socialId,
            final SocialType socialType) {
        return memberRepository.findBySocialTypeAndSocialId(socialId, socialType).isPresent();
    }

    private Long getMemberIdBySocialInfo(
            Long socialId,
            SocialType socialType) {
        return getMemberBySocialId(socialId, socialType).getId();
    }

    private LoginSuccessResponse createLoginSuccessResponse(
            Long memberId,
            MemberInfoResponse memberInfoResponse) {
        String nickname = memberInfoResponse.nickname();
        MemberAuthentication memberAuthentication = new MemberAuthentication(memberId, null, null);

        String refreshToken = issueAndSaveRefreshToken(memberId, memberAuthentication);
        String accessToken = jwtTokenProvider.issueAccessToken(memberAuthentication);

        return LoginSuccessResponse.of(accessToken, refreshToken, nickname);
    }

    public Member getMemberBySocialId(
            final Long socialId,
            final SocialType socialType) {
        Member member = memberRepository.findBySocialTypeAndSocialId(socialId, socialType)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
        return member;
    }
}