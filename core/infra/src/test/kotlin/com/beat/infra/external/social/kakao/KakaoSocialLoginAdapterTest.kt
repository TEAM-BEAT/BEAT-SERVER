package com.beat.infra.external.social.kakao

import com.beat.application.frontoffice.member.command.SocialLoginFailure
import com.beat.application.frontoffice.member.command.SocialLoginRequest
import com.beat.application.frontoffice.member.command.SocialLoginType
import com.beat.infra.external.social.kakao.client.KakaoApiClient
import com.beat.infra.external.social.kakao.client.KakaoAuthApiClient
import com.beat.infra.external.social.kakao.response.KakaoAccessTokenResponse
import com.beat.infra.external.social.kakao.response.KakaoAccount
import com.beat.infra.external.social.kakao.response.KakaoUserProfile
import com.beat.infra.external.social.kakao.response.KakaoUserResponse
import feign.FeignException
import feign.RetryableException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.SocketTimeoutException
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as given
import org.springframework.test.util.ReflectionTestUtils

class KakaoSocialLoginAdapterTest : FunSpec({

    var kakaoApiClient = mock(KakaoApiClient::class.java)
    var kakaoAuthApiClient = mock(KakaoAuthApiClient::class.java)
    var adapter = KakaoSocialLoginAdapter(kakaoApiClient, kakaoAuthApiClient)

    beforeTest {
        kakaoApiClient = mock(KakaoApiClient::class.java)
        kakaoAuthApiClient = mock(KakaoAuthApiClient::class.java)
        adapter = KakaoSocialLoginAdapter(kakaoApiClient, kakaoAuthApiClient)
        ReflectionTestUtils.setField(adapter, "clientId", "client-id")
        ReflectionTestUtils.setField(adapter, "redirectUri", "redirect-uri")
    }

    test("provider server error is not misclassified as authentication failure") {
        val providerException = mock(FeignException::class.java)
        given(providerException.status()).thenReturn(503)
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(providerException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE
        (failure.cause === providerException) shouldBe true
    }

    test("provider rate limit is unavailable instead of authentication failure") {
        val rateLimitException = mock(FeignException::class.java)
        given(rateLimitException.status()).thenReturn(429)
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(rateLimitException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE
        (failure.cause === rateLimitException) shouldBe true
    }

    test("provider configuration error is provider failure") {
        val providerException = mock(FeignException::class.java)
        given(providerException.status()).thenReturn(403)
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(providerException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === providerException) shouldBe true
    }

    test("rejected authorization code is authentication failure") {
        val authenticationException = mock(FeignException::class.java)
        given(authenticationException.status()).thenReturn(400)
        given(authenticationException.contentUTF8())
            .thenReturn("{\"error\":\"invalid_grant\",\"error_code\":\"KOE320\"}")
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(authenticationException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.AUTHENTICATION_FAILED
        (failure.cause === authenticationException) shouldBe true
    }

    test("provider configuration bad request is not authentication failure") {
        val configurationException = mock(FeignException::class.java)
        given(configurationException.status()).thenReturn(400)
        given(configurationException.contentUTF8())
            .thenReturn("{\"error\":\"invalid_client\",\"error_code\":\"KOE101\"}")
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(configurationException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === configurationException) shouldBe true
    }

    test("rejected provider access token is provider failure") {
        val authenticationException = mock(FeignException::class.java)
        given(authenticationException.status()).thenReturn(401)
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(successfulTokenResponse())
        given(kakaoApiClient.getUserInformation("Bearer access-token")).thenThrow(authenticationException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === authenticationException) shouldBe true
    }

    test("user info provider error is provider failure") {
        val providerException = mock(FeignException::class.java)
        given(providerException.status()).thenReturn(403)
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(successfulTokenResponse())
        given(kakaoApiClient.getUserInformation("Bearer access-token")).thenThrow(providerException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === providerException) shouldBe true
    }

    test("retryable timeout is classified separately") {
        val timeoutException = mock(RetryableException::class.java)
        given(timeoutException.cause).thenReturn(SocketTimeoutException("timeout"))
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(timeoutException)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_TIMEOUT
        (failure.cause === timeoutException) shouldBe true
    }

    test("missing token response is provider failure") {
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(null)

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
    }

    test("successful response maps member profile") {
        given(kakaoAuthApiClient.getOAuth2AccessToken(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(successfulTokenResponse())
        given(kakaoApiClient.getUserInformation("Bearer access-token")).thenReturn(
            KakaoUserResponse(
                id = 123L,
                connectedAt = null,
                kakaoAccount = KakaoAccount(
                    profileNicknameNeedsAgreement = false,
                    profile = KakaoUserProfile("nickname", null, null, false),
                    emailNeedsAgreement = false,
                    emailValid = true,
                    emailVerified = true,
                    email = "member@example.com",
                ),
            ),
        )

        val profile = adapter.login(kakaoRequest())

        profile.socialId shouldBe 123L
        profile.nickname shouldBe "nickname"
        profile.email shouldBe "member@example.com"
    }

})

private fun kakaoRequest(): SocialLoginRequest =
    SocialLoginRequest("authorization-code", SocialLoginType.KAKAO)

private fun successfulTokenResponse(): KakaoAccessTokenResponse =
    KakaoAccessTokenResponse("Bearer", "access-token", 3600, "refresh-token", 7200)
