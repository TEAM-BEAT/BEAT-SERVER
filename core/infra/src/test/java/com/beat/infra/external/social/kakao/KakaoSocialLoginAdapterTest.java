package com.beat.infra.external.social.kakao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.contracts.auth.social.SocialLoginFailure;
import com.beat.contracts.auth.social.SocialLoginRequest;
import com.beat.contracts.auth.social.SocialLoginType;
import com.beat.infra.external.social.kakao.client.KakaoApiClient;
import com.beat.infra.external.social.kakao.client.KakaoAuthApiClient;
import com.beat.infra.external.social.kakao.response.KakaoAccessTokenResponse;

import feign.FeignException;
import feign.RetryableException;

@ExtendWith(MockitoExtension.class)
class KakaoSocialLoginAdapterTest {

	@Mock
	private KakaoApiClient kakaoApiClient;

	@Mock
	private KakaoAuthApiClient kakaoAuthApiClient;

	private KakaoSocialLoginAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new KakaoSocialLoginAdapter(kakaoApiClient, kakaoAuthApiClient);
		ReflectionTestUtils.setField(adapter, "clientId", "client-id");
		ReflectionTestUtils.setField(adapter, "redirectUri", "redirect-uri");
	}

	@Test
	void providerServerErrorIsNotMisclassifiedAsAuthenticationFailure() {
		FeignException providerException = mock(FeignException.class);
		when(providerException.status()).thenReturn(503);
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(providerException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE, failure.getReason());
		assertSame(providerException, failure.getCause());
	}

	@Test
	void providerRateLimitIsUnavailableInsteadOfAuthenticationFailure() {
		FeignException rateLimitException = mock(FeignException.class);
		when(rateLimitException.status()).thenReturn(429);
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(rateLimitException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE, failure.getReason());
		assertSame(rateLimitException, failure.getCause());
	}

	@Test
	void providerConfigurationErrorIsBadGatewayInsteadOfAuthenticationFailure() {
		FeignException providerException = mock(FeignException.class);
		when(providerException.status()).thenReturn(403);
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(providerException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_FAILURE, failure.getReason());
		assertSame(providerException, failure.getCause());
	}

	@Test
	void rejectedAuthorizationCodeIsAuthenticationFailure() {
		FeignException authenticationException = mock(FeignException.class);
		when(authenticationException.status()).thenReturn(400);
		when(authenticationException.contentUTF8())
			.thenReturn("{\"error\":\"invalid_grant\",\"error_code\":\"KOE320\"}");
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(authenticationException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.AUTHENTICATION_FAILED, failure.getReason());
		assertSame(authenticationException, failure.getCause());
	}

	@Test
	void providerConfigurationBadRequestIsNotAuthenticationFailure() {
		FeignException configurationException = mock(FeignException.class);
		when(configurationException.status()).thenReturn(400);
		when(configurationException.contentUTF8())
			.thenReturn("{\"error\":\"invalid_client\",\"error_code\":\"KOE101\"}");
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(configurationException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_FAILURE, failure.getReason());
		assertSame(configurationException, failure.getCause());
	}

	@Test
	void rejectedProviderAccessTokenIsProviderFailure() {
		FeignException authenticationException = mock(FeignException.class);
		when(authenticationException.status()).thenReturn(401);
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenReturn(successfulTokenResponse());
		when(kakaoApiClient.getUserInformation("Bearer access-token")).thenThrow(authenticationException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_FAILURE, failure.getReason());
		assertSame(authenticationException, failure.getCause());
	}

	@Test
	void userInfoProviderErrorIsBadGatewayInsteadOfAuthenticationFailure() {
		FeignException providerException = mock(FeignException.class);
		when(providerException.status()).thenReturn(403);
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenReturn(successfulTokenResponse());
		when(kakaoApiClient.getUserInformation("Bearer access-token")).thenThrow(providerException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_FAILURE, failure.getReason());
		assertSame(providerException, failure.getCause());
	}

	@Test
	void retryableTimeoutIsClassifiedSeparately() {
		RetryableException timeoutException = mock(RetryableException.class);
		when(timeoutException.getCause()).thenReturn(new SocketTimeoutException("timeout"));
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(timeoutException);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_TIMEOUT, failure.getReason());
		assertSame(timeoutException, failure.getCause());
	}

	@Test
	void missingTokenResponseIsProviderFailure() {
		when(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
			.thenReturn(null);

		SocialLoginFailure failure = assertThrows(SocialLoginFailure.class, () -> adapter.login(kakaoRequest()));

		assertEquals(SocialLoginFailure.Reason.PROVIDER_FAILURE, failure.getReason());
	}

	private static SocialLoginRequest kakaoRequest() {
		return new SocialLoginRequest("authorization-code", SocialLoginType.KAKAO);
	}

	private static KakaoAccessTokenResponse successfulTokenResponse() {
		return new KakaoAccessTokenResponse("Bearer", "access-token", 3600, "refresh-token", 7200);
	}
}
