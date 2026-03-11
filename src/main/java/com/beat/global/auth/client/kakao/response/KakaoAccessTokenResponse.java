package com.beat.global.auth.client.kakao.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoAccessTokenResponse(
	@JsonProperty("token_type")
	String tokenType,

	@JsonProperty("access_token")
	String accessToken,

	@JsonProperty("refresh_token")
	String refreshToken,

	@JsonProperty("expires_in")
	Long expiresIn,

	@JsonProperty("refresh_token_expires_in")
	Long refreshTokenExpiresIn
) {
}
