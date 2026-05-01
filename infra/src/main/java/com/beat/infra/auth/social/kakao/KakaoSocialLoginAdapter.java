package com.beat.infra.auth.social.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialLoginRequest;
import com.beat.contracts.auth.social.SocialLoginType;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.infra.auth.social.kakao.client.KakaoApiClient;
import com.beat.infra.auth.social.kakao.client.KakaoAuthApiClient;
import com.beat.infra.auth.social.kakao.response.KakaoAccessTokenResponse;
import com.beat.infra.auth.social.kakao.response.KakaoUserResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoSocialLoginAdapter implements SocialLoginPort {

	private static final String AUTH_CODE = "authorization_code";

	@Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
	private String redirectUri;

	@Value("${spring.security.oauth2.client.registration.kakao.client-id}")
	private String clientId;

	private final KakaoApiClient kakaoApiClient;
	private final KakaoAuthApiClient kakaoAuthApiClient;

	@Override
	public SocialMemberInfo login(SocialLoginRequest request) {
		if (request.socialType() != SocialLoginType.KAKAO) {
			throw SocialLoginFailure.unsupportedSocialType();
		}

		String accessToken;
		try {
			accessToken = getOAuth2Authentication(request.authorizationCode());
		} catch (FeignException exception) {
			throw SocialLoginFailure.authenticationFailed(exception);
		}

		try {
			return mapToSocialMemberInfo(getUserInfo(accessToken));
		} catch (FeignException exception) {
			log.error("Failed to fetch Kakao user info with access token", exception);
			throw SocialLoginFailure.authenticationFailed(exception);
		}
	}

	private String getOAuth2Authentication(String authorizationCode) {
		KakaoAccessTokenResponse response = kakaoAuthApiClient.getOAuth2AccessToken(
			AUTH_CODE,
			clientId,
			redirectUri,
			authorizationCode
		);
		if (response == null) {
			log.error("Kakao OAuth token response is null.");
			throw SocialLoginFailure.authenticationFailed(null);
		}

		log.info("Received OAuth2 authentication response: tokenType={}, hasAccessToken={}, hasRefreshToken={}",
			response.tokenType(),
			response.accessToken() != null && !response.accessToken().isBlank(),
			response.refreshToken() != null && !response.refreshToken().isBlank());

		String accessToken = response.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			log.error("Kakao OAuth token response does not contain access token. response={}", response);
			throw SocialLoginFailure.authenticationFailed(null);
		}
		return accessToken;
	}

	private KakaoUserResponse getUserInfo(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw SocialLoginFailure.authenticationFailed(null);
		}

		KakaoUserResponse kakaoUserResponse = kakaoApiClient.getUserInformation("Bearer " + accessToken);
		log.info("Kakao user response summary: id={}, hasKakaoAccount={}, hasProfile={}",
			kakaoUserResponse != null ? kakaoUserResponse.id() : null,
			kakaoUserResponse != null && kakaoUserResponse.kakaoAccount() != null,
			kakaoUserResponse != null
				&& kakaoUserResponse.kakaoAccount() != null
				&& kakaoUserResponse.kakaoAccount().profile() != null);
		return kakaoUserResponse;
	}

	private SocialMemberInfo mapToSocialMemberInfo(KakaoUserResponse kakaoUserResponse) {
		if (kakaoUserResponse == null) {
			throw SocialLoginFailure.authenticationFailed(null);
		}
		if (kakaoUserResponse.id() == null) {
			log.error("Kakao user response does not contain id.");
			throw SocialLoginFailure.authenticationFailed(null);
		}
		if (kakaoUserResponse.kakaoAccount() == null) {
			log.error("Kakao user response does not contain kakao_account. id={}", kakaoUserResponse.id());
			throw SocialLoginFailure.authenticationFailed(null);
		}
		if (kakaoUserResponse.kakaoAccount().profile() == null) {
			log.error("Kakao user response does not contain profile. id={}", kakaoUserResponse.id());
			throw SocialLoginFailure.authenticationFailed(null);
		}

		String nickname = kakaoUserResponse.kakaoAccount().profile().nickname();
		String email = kakaoUserResponse.kakaoAccount().email();
		if (nickname == null || nickname.isBlank()) {
			log.error("Kakao user response does not contain nickname. id={}", kakaoUserResponse.id());
			throw SocialLoginFailure.authenticationFailed(null);
		}

		return new SocialMemberInfo(
			kakaoUserResponse.id(),
			nickname,
			email
		);
	}
}
