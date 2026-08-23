package com.beat.infra.external.social.kakao.client

import com.beat.infra.external.social.kakao.response.KakaoUserResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(name = "kakaoApiClient", url = "https://kapi.kakao.com")
internal interface KakaoApiClient {
    @GetMapping(value = ["/v2/user/me"])
    fun getUserInformation(
        @RequestHeader(HttpHeaders.AUTHORIZATION) accessToken: String,
    ): KakaoUserResponse?
}
