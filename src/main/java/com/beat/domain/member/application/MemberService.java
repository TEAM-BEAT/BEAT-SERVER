package com.beat.domain.member.application;

import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.dto.*;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.client.service.KakaoSocialService;
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
@Transactional(readOnly = false)
@Service
public class MemberService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final KakaoSocialService kakaoSocialService;

    public LoginSuccessResponse create(
            final String authorizationCode,
            final MemberLoginRequest loginRequest
    ) {
        return getTokenDto(getUserInfoResponse(authorizationCode, loginRequest));
    }

    public MemberInfoResponse getUserInfoResponse(
            final String authorizationCode,
            final MemberLoginRequest loginRequest
    ) {
        switch (loginRequest.socialType()) {
            case KAKAO:
                return kakaoSocialService.login(authorizationCode, loginRequest);
            default:
                throw new BadRequestException(MemberErrorCode.SOCIAL_TYPE_BAD_REQUEST);
        }
    }

    @Transactional
    public Long createUser(final MemberInfoResponse userResponse) {
        // Users 엔티티를 먼저 생성
        Users users = Users.create();
        users = userRepository.save(users);

        // Users 엔티티가 생성된 후 Member 엔티티 생성
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

    public Member getBySocialId(
            final Long socialId,
            final SocialType socialType) {
        Member member = memberRepository.findBySocialTypeAndSocialId(socialId, socialType).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND)
        );
        return member;
    }

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

    public boolean isExistingMember(
            final Long socialId,
            final SocialType socialType
    ) {
        return memberRepository.findBySocialTypeAndSocialId(socialId, socialType).isPresent();
    }

    public LoginSuccessResponse getTokenByMemberId(
            final Long id,
            final MemberInfoResponse memberInfoResponse
    ) {
        MemberAuthentication memberAuthentication = new MemberAuthentication(id, null, null);
        String refreshToken = jwtTokenProvider.issueRefreshToken(memberAuthentication);
        tokenService.saveRefreshToken(id, refreshToken);
        String nickname = memberInfoResponse.nickname();
        return LoginSuccessResponse.of(
                jwtTokenProvider.issueAccessToken(memberAuthentication),
                refreshToken, nickname
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

    private LoginSuccessResponse getTokenDto(
            final MemberInfoResponse userResponse
    ) {
        if (isExistingMember(userResponse.socialId(), userResponse.socialType())) {
            return getTokenByMemberId(getBySocialId(userResponse.socialId(), userResponse.socialType()).getId(), userResponse);
        } else {
            Long id = createUser(userResponse);
            return getTokenByMemberId(id, userResponse);
        }
    }
}