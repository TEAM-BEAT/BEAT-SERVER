package com.beat.global.auth.client.kakao.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(
	@JsonProperty("id")
	Long id,

	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {
}
