package com.beat.infra.external.auth.social.kakao.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
	@JsonProperty("id")
	Long id,
	@JsonProperty("connected_at")
	String connectedAt,
	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {
}
