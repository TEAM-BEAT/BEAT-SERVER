package com.beat.infrastructure.external.social.kakao.client

import com.beat.infrastructure.external.social.kakao.response.KakaoAccessTokenResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "kakaoAuthApiClient", url = "https://kauth.kakao.com")
internal interface KakaoAuthApiClient {
    @PostMapping(value = ["/oauth/token"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun getOAuth2AccessToken(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("redirect_uri") redirectUri: String,
        @RequestParam("code") code: String,
    ): KakaoAccessTokenResponse?
}
