package com.beat.infra.external.auth.social.kakao.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserProfile(
	@JsonProperty("nickname")
	String nickname,
	@JsonProperty("thumbnail_image_url")
	String thumbnailImageUrl,
	@JsonProperty("profile_image_url")
	String profileImageUrl,
	@JsonProperty("is_default_image")
	Boolean defaultImage
) {
}
