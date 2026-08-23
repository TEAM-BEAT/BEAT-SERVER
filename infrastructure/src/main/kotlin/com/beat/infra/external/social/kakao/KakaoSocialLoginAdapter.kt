package com.beat.infra.external.social.kakao

import com.beat.application.frontoffice.member.command.SocialLoginFailure
import com.beat.application.frontoffice.member.command.SocialLoginProfile
import com.beat.application.frontoffice.member.command.SocialLoginProvider
import com.beat.application.frontoffice.member.command.SocialLoginRequest
import com.beat.application.frontoffice.member.command.SocialLoginType
import com.beat.infra.external.social.kakao.client.KakaoApiClient
import com.beat.infra.external.social.kakao.client.KakaoAuthApiClient
import com.beat.infra.external.social.kakao.response.KakaoUserResponse
import feign.FeignException
import feign.RetryableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import java.util.regex.Pattern

@Service
internal class KakaoSocialLoginAdapter(
    private val kakaoApiClient: KakaoApiClient,
    private val kakaoAuthApiClient: KakaoAuthApiClient,
) : SocialLoginProvider {
    @field:Value("\${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private lateinit var redirectUri: String

    @field:Value("\${spring.security.oauth2.client.registration.kakao.client-id}")
    private lateinit var clientId: String

    override fun login(request: SocialLoginRequest): SocialLoginProfile {
        if (request.socialType != SocialLoginType.KAKAO) {
            throw SocialLoginFailure.unsupportedSocialType()
        }

        val accessToken =
            try {
                getOAuth2Authentication(request.authorizationCode)
            } catch (exception: RetryableException) {
                throw translateRetryableFailure(exception)
            } catch (exception: FeignException) {
                throw translateTokenFeignFailure(exception)
            }

        try {
            return mapToSocialLoginProfile(getUserInfo(accessToken))
        } catch (exception: RetryableException) {
            throw translateRetryableFailure(exception)
        } catch (exception: FeignException) {
            throw translateUserInfoFeignFailure(exception)
        }
    }

    private fun getOAuth2Authentication(authorizationCode: String): String {
        val response = kakaoAuthApiClient.getOAuth2AccessToken(AUTH_CODE, clientId, redirectUri, authorizationCode)
        if (response == null) {
            log.error("Kakao OAuth token response is null.")
            throw SocialLoginFailure.providerFailure()
        }

        log.info(
            "Received OAuth2 authentication response: tokenType={}, hasAccessToken={}, hasRefreshToken={}",
            response.tokenType,
            !response.accessToken.isNullOrBlank(),
            !response.refreshToken.isNullOrBlank(),
        )

        val accessToken = response.accessToken
        if (accessToken.isNullOrBlank()) {
            log.error("Kakao OAuth token response does not contain access token.")
            throw SocialLoginFailure.providerFailure()
        }
        return accessToken
    }

    private fun getUserInfo(accessToken: String): KakaoUserResponse? {
        if (accessToken.isBlank()) {
            throw SocialLoginFailure.providerFailure()
        }

        val response = kakaoApiClient.getUserInformation("Bearer $accessToken")
        log.info(
            "Kakao user response summary: hasId={}, hasKakaoAccount={}, hasProfile={}",
            response?.id != null,
            response?.kakaoAccount != null,
            response?.kakaoAccount?.profile != null,
        )
        return response
    }

    private fun mapToSocialLoginProfile(response: KakaoUserResponse?): SocialLoginProfile {
        if (response == null) {
            throw SocialLoginFailure.providerFailure()
        }
        val id = response.id
        if (id == null) {
            log.error("Kakao user response does not contain id.")
            throw SocialLoginFailure.providerFailure()
        }
        val account = response.kakaoAccount
        if (account == null) {
            log.error("Kakao user response does not contain kakao_account.")
            throw SocialLoginFailure.providerFailure()
        }
        val profile = account.profile
        if (profile == null) {
            log.error("Kakao user response does not contain profile.")
            throw SocialLoginFailure.providerFailure()
        }

        val nickname = profile.nickname
        if (nickname.isNullOrBlank()) {
            log.error("Kakao user response does not contain nickname.")
            throw SocialLoginFailure.providerFailure()
        }

        val email = account.email
        if (email.isNullOrBlank()) {
            log.error("Kakao user response does not contain email.")
            throw SocialLoginFailure.providerFailure()
        }

        return SocialLoginProfile(id, nickname, email)
    }

    private fun translateTokenFeignFailure(exception: FeignException): SocialLoginFailure {
        log.warn("Kakao OAuth token request failed: status={}", exception.status())
        if ((exception.status() == 400 || exception.status() == 401) &&
            REJECTED_AUTHORIZATION_CODE.matcher(exception.contentUTF8()).find()
        ) {
            return SocialLoginFailure.authenticationFailed(exception)
        }
        return translateProviderFeignFailure(exception)
    }

    private fun translateUserInfoFeignFailure(exception: FeignException): SocialLoginFailure {
        log.warn("Kakao user-info request failed: status={}", exception.status())
        return translateProviderFeignFailure(exception)
    }

    private fun translateProviderFeignFailure(exception: FeignException): SocialLoginFailure =
        if (exception.status() == 429 || exception.status() >= 500 || exception.status() < 0) {
            SocialLoginFailure.providerUnavailable(exception)
        } else {
            SocialLoginFailure.providerFailure(exception)
        }

    private fun translateRetryableFailure(exception: RetryableException): SocialLoginFailure {
        val timedOut = generateSequence(exception as Throwable) { it.cause }
            .any { it is SocketTimeoutException }
        if (timedOut) {
            log.warn("Kakao request timed out: status={}", exception.status())
            return SocialLoginFailure.providerTimeout(exception)
        }
        log.warn("Kakao retryable request failed: status={}", exception.status())
        return SocialLoginFailure.providerUnavailable(exception)
    }

    private companion object {
        val log = LoggerFactory.getLogger(KakaoSocialLoginAdapter::class.java)
        val REJECTED_AUTHORIZATION_CODE: Pattern = Pattern.compile("\\\"error_code\\\"\\s*:\\s*\\\"?KOE320\\\"?")
        const val AUTH_CODE = "authorization_code"
    }
}
