package com.beat.infra.auth.social.kakao;

import com.beat.contracts.auth.TokenErrorCode;
import com.beat.contracts.auth.social.SocialLoginCommand;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.UnauthorizedException;
import com.beat.infra.auth.social.kakao.client.KakaoApiClient;
import com.beat.infra.auth.social.kakao.client.KakaoAuthApiClient;
import com.beat.infra.auth.social.kakao.response.KakaoAccessTokenResponse;
import com.beat.infra.auth.social.kakao.response.KakaoUserResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
	public SocialMemberInfo login(SocialLoginCommand command) {
		if (command.socialType() != SocialType.KAKAO) {
			throw new BadRequestException(MemberErrorCode.SOCIAL_TYPE_BAD_REQUEST);
		}

		String accessToken;
		try {
			accessToken = getOAuth2Authentication(command.authorizationCode());
		} catch (FeignException exception) {
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		try {
			return mapToSocialMemberInfo(command.socialType(), getUserInfo(accessToken));
		} catch (FeignException exception) {
			log.error("Failed to fetch Kakao user info with access token", exception);
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
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
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}

		log.info("Received OAuth2 authentication response: tokenType={}, hasAccessToken={}, hasRefreshToken={}",
			response.tokenType(),
			response.accessToken() != null && !response.accessToken().isBlank(),
			response.refreshToken() != null && !response.refreshToken().isBlank());

		String accessToken = response.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			log.error("Kakao OAuth token response does not contain access token. response={}", response);
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}
		return accessToken;
	}

	private KakaoUserResponse getUserInfo(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
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

	private SocialMemberInfo mapToSocialMemberInfo(SocialType socialType, KakaoUserResponse kakaoUserResponse) {
		if (kakaoUserResponse == null) {
			throw new UnauthorizedException(TokenErrorCode.AUTHENTICATION_CODE_EXPIRED);
		}
		if (kakaoUserResponse.id() == null) {
			log.error("Kakao user response does not contain id.");
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

		return new SocialMemberInfo(
			kakaoUserResponse.id(),
			nickname,
			email,
			socialType
		);
	}
}
