package com.beat.infrastructure.external.social.kakao

import com.beat.application.frontoffice.member.command.SocialLoginFailure
import com.beat.application.frontoffice.member.command.SocialLoginRequest
import com.beat.application.frontoffice.member.command.SocialLoginType
import com.beat.infrastructure.external.social.kakao.client.KakaoApiClient
import com.beat.infrastructure.external.social.kakao.client.KakaoAuthApiClient
import com.beat.infrastructure.external.social.kakao.response.KakaoAccessTokenResponse
import com.beat.infrastructure.external.social.kakao.response.KakaoAccount
import com.beat.infrastructure.external.social.kakao.response.KakaoUserProfile
import com.beat.infrastructure.external.social.kakao.response.KakaoUserResponse
import feign.FeignException
import feign.RetryableException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.SocketTimeoutException
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils

class KakaoSocialLoginAdapterTest : FunSpec({

    var kakaoApiClient = mockk<KakaoApiClient>(relaxed = true)
    var kakaoAuthApiClient = mockk<KakaoAuthApiClient>(relaxed = true)
    var adapter = KakaoSocialLoginAdapter(kakaoApiClient, kakaoAuthApiClient)

    beforeTest {
        kakaoApiClient = mockk(relaxed = true)
        kakaoAuthApiClient = mockk(relaxed = true)
        adapter = KakaoSocialLoginAdapter(kakaoApiClient, kakaoAuthApiClient)
        ReflectionTestUtils.setField(adapter, "clientId", "client-id")
        ReflectionTestUtils.setField(adapter, "redirectUri", "redirect-uri")
    }

    test("provider 서버 오류는 인증 실패로 잘못 분류되지 않는다") {
        val providerException = mockk<FeignException>(relaxed = true)
        every { providerException.status() } returns 503
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } throws providerException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE
        (failure.cause === providerException) shouldBe true
    }

    test("provider rate limit은 인증 실패가 아닌 unavailable로 분류된다") {
        val rateLimitException = mockk<FeignException>(relaxed = true)
        every { rateLimitException.status() } returns 429
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } throws rateLimitException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE
        (failure.cause === rateLimitException) shouldBe true
    }

    test("provider 설정 오류는 provider failure로 분류된다") {
        val providerException = mockk<FeignException>(relaxed = true)
        every { providerException.status() } returns 403
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } throws providerException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === providerException) shouldBe true
    }

    test("거부된 authorization code는 인증 실패로 분류된다") {
        val authenticationException = mockk<FeignException>(relaxed = true)
        every { authenticationException.status() } returns 400
        every { authenticationException.contentUTF8() }
            .returns("{\"error\":\"invalid_grant\",\"error_code\":\"KOE320\"}")
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } throws authenticationException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.AUTHENTICATION_FAILED
        (failure.cause === authenticationException) shouldBe true
    }

    test("provider 설정 bad request는 인증 실패로 분류되지 않는다") {
        val configurationException = mockk<FeignException>(relaxed = true)
        every { configurationException.status() } returns 400
        every { configurationException.contentUTF8() }
            .returns("{\"error\":\"invalid_client\",\"error_code\":\"KOE101\"}")
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } throws configurationException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === configurationException) shouldBe true
    }

    test("거부된 provider access token은 provider failure로 분류된다") {
        val authenticationException = mockk<FeignException>(relaxed = true)
        every { authenticationException.status() } returns 401
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } returns successfulTokenResponse()
        every { kakaoApiClient.getUserInformation("Bearer access-token") } throws authenticationException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === authenticationException) shouldBe true
    }

    test("user info provider 오류는 provider failure로 분류된다") {
        val providerException = mockk<FeignException>(relaxed = true)
        every { providerException.status() } returns 403
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } returns successfulTokenResponse()
        every { kakaoApiClient.getUserInformation("Bearer access-token") } throws providerException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
        (failure.cause === providerException) shouldBe true
    }

    test("재시도 가능한 timeout은 별도로 분류된다") {
        val timeoutException = mockk<RetryableException>(relaxed = true)
        every { timeoutException.cause } returns SocketTimeoutException("timeout")
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } throws timeoutException

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_TIMEOUT
        (failure.cause === timeoutException) shouldBe true
    }

    test("token 응답이 없으면 provider failure로 분류된다") {
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } returns null

        val failure = shouldThrow<SocialLoginFailure> { adapter.login(kakaoRequest()) }

        failure.reason shouldBe SocialLoginFailure.Reason.PROVIDER_FAILURE
    }

    test("성공 응답은 member profile로 매핑된다") {
        every {
            kakaoAuthApiClient.getOAuth2AccessToken(any(), any(), any(), any())
        } returns successfulTokenResponse()
        every {
            kakaoApiClient.getUserInformation("Bearer access-token")
        } returns
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
