package com.beat.infra.external.auth.social.kakao;

import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginPort;
import com.beat.contracts.auth.social.SocialLoginRequest;
import com.beat.contracts.auth.social.SocialLoginType;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.infra.external.auth.social.kakao.client.KakaoApiClient;
import com.beat.infra.external.auth.social.kakao.client.KakaoAuthApiClient;
import com.beat.infra.external.auth.social.kakao.response.KakaoAccessTokenResponse;
import com.beat.infra.external.auth.social.kakao.response.KakaoUserResponse;

import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoSocialLoginAdapter implements SocialLoginPort {
	private static final Pattern REJECTED_AUTHORIZATION_CODE =
		Pattern.compile("\\\"error_code\\\"\\s*:\\s*\\\"?KOE320\\\"?");

	private static final String AUTH_CODE = "authorization_code";

	@Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
	private String redirectUri;

	@Value("${spring.security.oauth2.client.registration.kakao.client-id}")
	private String clientId;

	private final KakaoApiClient kakaoApiClient;
	private final KakaoAuthApiClient kakaoAuthApiClient;

	@Override
	public SocialMemberInfo login(SocialLoginRequest request) {
		if (request.getSocialType() != SocialLoginType.KAKAO) {
			throw SocialLoginFailure.unsupportedSocialType();
		}

		String accessToken;
		try {
			accessToken = getOAuth2Authentication(request.getAuthorizationCode());
		} catch (RetryableException exception) {
			throw translateRetryableFailure(exception);
		} catch (FeignException exception) {
			throw translateTokenFeignFailure(exception);
		}

		try {
			return mapToSocialMemberInfo(getUserInfo(accessToken));
		} catch (RetryableException exception) {
			throw translateRetryableFailure(exception);
		} catch (FeignException exception) {
			throw translateUserInfoFeignFailure(exception);
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
			throw SocialLoginFailure.providerFailure();
		}

		log.info("Received OAuth2 authentication response: tokenType={}, hasAccessToken={}, hasRefreshToken={}",
			response.tokenType(),
			response.accessToken() != null && !response.accessToken().isBlank(),
			response.refreshToken() != null && !response.refreshToken().isBlank());

		String accessToken = response.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			log.error("Kakao OAuth token response does not contain access token.");
			throw SocialLoginFailure.providerFailure();
		}
		return accessToken;
	}

	private KakaoUserResponse getUserInfo(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw SocialLoginFailure.providerFailure();
		}

		KakaoUserResponse kakaoUserResponse = kakaoApiClient.getUserInformation("Bearer " + accessToken);
		log.info("Kakao user response summary: hasId={}, hasKakaoAccount={}, hasProfile={}",
			kakaoUserResponse != null && kakaoUserResponse.id() != null,
			kakaoUserResponse != null && kakaoUserResponse.kakaoAccount() != null,
			kakaoUserResponse != null
				&& kakaoUserResponse.kakaoAccount() != null
				&& kakaoUserResponse.kakaoAccount().profile() != null);
		return kakaoUserResponse;
	}

	private SocialMemberInfo mapToSocialMemberInfo(KakaoUserResponse kakaoUserResponse) {
		if (kakaoUserResponse == null) {
			throw SocialLoginFailure.providerFailure();
		}
		if (kakaoUserResponse.id() == null) {
			log.error("Kakao user response does not contain id.");
			throw SocialLoginFailure.providerFailure();
		}
		if (kakaoUserResponse.kakaoAccount() == null) {
			log.error("Kakao user response does not contain kakao_account.");
			throw SocialLoginFailure.providerFailure();
		}
		if (kakaoUserResponse.kakaoAccount().profile() == null) {
			log.error("Kakao user response does not contain profile.");
			throw SocialLoginFailure.providerFailure();
		}

		String nickname = kakaoUserResponse.kakaoAccount().profile().nickname();
		String email = kakaoUserResponse.kakaoAccount().email();
		if (nickname == null || nickname.isBlank()) {
			log.error("Kakao user response does not contain nickname.");
			throw SocialLoginFailure.providerFailure();
		}

		return new SocialMemberInfo(
			kakaoUserResponse.id(),
			nickname,
			email
		);
	}

	private SocialLoginFailure translateTokenFeignFailure(FeignException exception) {
		log.warn("Kakao OAuth token request failed: status={}", exception.status());
		if ((exception.status() == 400 || exception.status() == 401)
			&& REJECTED_AUTHORIZATION_CODE.matcher(exception.contentUTF8()).find()) {
			return SocialLoginFailure.authenticationFailed(exception);
		}
		return translateProviderFeignFailure(exception);
	}

	private SocialLoginFailure translateUserInfoFeignFailure(FeignException exception) {
		log.warn("Kakao user-info request failed: status={}", exception.status());
		return translateProviderFeignFailure(exception);
	}

	private SocialLoginFailure translateProviderFeignFailure(FeignException exception) {
		if (exception.status() == 429 || exception.status() >= 500 || exception.status() < 0) {
			return SocialLoginFailure.providerUnavailable(exception);
		}
		return SocialLoginFailure.providerFailure(exception);
	}

	private SocialLoginFailure translateRetryableFailure(RetryableException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof SocketTimeoutException) {
				log.warn("Kakao request timed out: status={}", exception.status());
				return SocialLoginFailure.providerTimeout(exception);
			}
			cause = cause.getCause();
		}
		log.warn("Kakao retryable request failed: status={}", exception.status());
		return SocialLoginFailure.providerUnavailable(exception);
	}
}
