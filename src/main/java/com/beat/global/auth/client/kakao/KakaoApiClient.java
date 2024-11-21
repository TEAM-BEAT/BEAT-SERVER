package com.beat.global.auth.client.kakao;

import com.beat.global.auth.client.kakao.response.KakaoUserResponse;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "kakaoApiClient", url = "https://kapi.kakao.com")
public interface KakaoApiClient {

	@Operation(summary = "카카오 개인정보 조회 API", description = "카카오로그인 결과 카카오 개인정보를 조회하는 GET API입니다.")
	@GetMapping(value = "/v2/user/me")
	KakaoUserResponse getUserInformation(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);
}