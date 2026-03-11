package com.beat.global.auth.client.application;

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

	@Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
	private String redirectUri;

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
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}
		// Access Token으로 유저 정보 불러오기
		try {
			return getLoginDto(loginRequest.socialType(), getUserInfo(accessToken));
		} catch (FeignException e) {
			log.error("Failed to fetch Kakao user info with access token", e);
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}
	}

	private String getOAuth2Authentication(
		final String authorizationCode
	) {
		KakaoAccessTokenResponse response = kakaoAuthApiClient.getOAuth2AccessToken(
			AUTH_CODE,
			clientId,
			redirectUri,
			authorizationCode
		);
		log.info("Received OAuth2 authentication response: tokenType={}, hasAccessToken={}, hasRefreshToken={}",
			response.tokenType(),
			response.accessToken() != null && !response.accessToken().isBlank(),
			response.refreshToken() != null && !response.refreshToken().isBlank()
		);

		String accessToken = response.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			log.error("Kakao OAuth token response does not contain access token. response={}", response);
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		return accessToken;
	}

	private KakaoUserResponse getUserInfo(
		final String accessToken
	) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		KakaoUserResponse kakaoUserResponse = kakaoApiClient.getUserInformation("Bearer " + accessToken);

		log.info("Kakao user response summary: id={}, hasKakaoAccount={}, hasProfile={}",
			kakaoUserResponse != null ? kakaoUserResponse.id() : null,
			kakaoUserResponse != null && kakaoUserResponse.kakaoAccount() != null,
			kakaoUserResponse != null && kakaoUserResponse.kakaoAccount() != null && kakaoUserResponse.kakaoAccount().profile() != null
		);

		return kakaoUserResponse;
	}

	private MemberInfoResponse getLoginDto(
		final SocialType socialType,
		final KakaoUserResponse kakaoUserResponse
	) {
		if (kakaoUserResponse == null) {
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		if (kakaoUserResponse.kakaoAccount() == null) {
			log.error("Kakao user response does not contain kakao_account. id={}", kakaoUserResponse.id());
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		if (kakaoUserResponse.kakaoAccount().profile() == null) {
			log.error("Kakao user response does not contain profile. id={}", kakaoUserResponse.id());
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		String nickname = kakaoUserResponse.kakaoAccount().profile().nickname();
		String email = kakaoUserResponse.kakaoAccount().email();

		if (nickname == null || nickname.isBlank()) {
			log.error("Kakao user response does not contain nickname. id={}", kakaoUserResponse.id());
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		return MemberInfoResponse.of(
			kakaoUserResponse.id(),
			nickname,
			email,
			socialType
		);
	}
}