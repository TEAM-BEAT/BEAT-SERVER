package com.beat.domain.member.application;

import com.beat.domain.member.dto.AccessTokenGetSuccess;
import com.beat.domain.member.dto.LoginSuccessResponse;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.auth.jwt.exception.TokenErrorCode;
import com.beat.global.auth.jwt.provider.JwtTokenProvider;
import com.beat.global.auth.security.MemberAuthentication;
import com.beat.global.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Transactional
    public AccessTokenGetSuccess generateAccessTokenFromRefreshToken(final String refreshToken) {
        Long memberId = jwtTokenProvider.getUserFromJwt(refreshToken);
        if (!memberId.equals(tokenService.findIdByRefreshToken(refreshToken))) {
            throw new BadRequestException(TokenErrorCode.TOKEN_INCORRECT_ERROR);
        }
        MemberAuthentication memberAuthentication = new MemberAuthentication(memberId, null, null);
        return AccessTokenGetSuccess.of(jwtTokenProvider.issueAccessToken(memberAuthentication));
    }

    public String issueAndSaveRefreshToken(Long id, MemberAuthentication memberAuthentication) {
        String refreshToken = jwtTokenProvider.issueRefreshToken(memberAuthentication);
        tokenService.saveRefreshToken(id, refreshToken);
        return refreshToken;
    }

    public LoginSuccessResponse generateLoginSuccessResponse(final Long memberId, final MemberInfoResponse memberInfoResponse) {
        String nickname = memberInfoResponse.nickname();
        MemberAuthentication memberAuthentication = new MemberAuthentication(memberId, null, null);

        String refreshToken = issueAndSaveRefreshToken(memberId, memberAuthentication);
        String accessToken = jwtTokenProvider.issueAccessToken(memberAuthentication);

        return LoginSuccessResponse.of(accessToken, refreshToken, nickname);
    }
}