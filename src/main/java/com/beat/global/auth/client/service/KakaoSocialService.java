package com.beat.global.auth.client.service;

import com.beat.domain.member.domain.SocialType;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.client.kakao.KakaoApiClient;
import com.beat.global.auth.client.kakao.KakaoAuthApiClient;
import com.beat.global.auth.client.kakao.response.KakaoAccessTokenResponse;
import com.beat.global.auth.client.kakao.response.KakaoUserResponse;
import com.beat.global.auth.jwt.exception.TokenErrorCode;
import com.beat.global.common.exception.UnauthorizedException;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoSocialService implements SocialService {

    private static final String AUTH_CODE = "authorization_code";
    private static final String REDIRECT_URI = "http://localhost:8080/kakao/callback";

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;
    private final KakaoApiClient kakaoApiClient;
    private final KakaoAuthApiClient kakaoAuthApiClient;


    @Transactional
    @Override
    public MemberInfoResponse login(
            final String authorizationCode,
            final MemberLoginRequest loginRequest
    ) {
        String accessToken;
        try {
            // 인가 코드로 Access Token + Refresh Token 받아오기
            accessToken = getOAuth2Authentication(authorizationCode);
        } catch (FeignException e) {
//            log.info("Error during OAuth2 authentication", e);
            throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
        }
        // Access Token으로 유저 정보 불러오기
        return getLoginDto(loginRequest.socialType(), getUserInfo(accessToken));
    }

    private String getOAuth2Authentication(
            final String authorizationCode
    ) {
//        log.debug("Requesting OAuth2 authentication with code: {}", authorizationCode);
        KakaoAccessTokenResponse response = kakaoAuthApiClient.getOAuth2AccessToken(
                AUTH_CODE,
                clientId,
                REDIRECT_URI,
                authorizationCode
        );
//        log.debug("Received OAuth2 authentication response: {}", response);
        return response.accessToken();
    }

    private KakaoUserResponse getUserInfo(
            final String accessToken
    ) {
//        log.debug("Requesting user information with access token: {}", accessToken);
        KakaoUserResponse kakaoUserResponse = kakaoApiClient.getUserInformation("Bearer " + accessToken);
//        log.debug("Received user information response: {}", userResponse);
        return kakaoUserResponse;
    }

    private MemberInfoResponse getLoginDto(
            final SocialType socialType,
            final KakaoUserResponse kakaoUserResponse
    ) {
        return MemberInfoResponse.of(
                kakaoUserResponse.id(),
                kakaoUserResponse.kakaoAccount().profile().nickname(),
                kakaoUserResponse.kakaoAccount().email(),
                socialType
        );
    }

}