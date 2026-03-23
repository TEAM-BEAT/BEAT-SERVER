package com.beat.infra.auth.social.kakao.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoAccount(
	@JsonProperty("profile_nickname_needs_agreement")
	Boolean profileNicknameNeedsAgreement,
	@JsonProperty("profile")
	KakaoUserProfile profile,
	@JsonProperty("email_needs_agreement")
	Boolean emailNeedsAgreement,
	@JsonProperty("is_email_valid")
	Boolean emailValid,
	@JsonProperty("is_email_verified")
	Boolean emailVerified,
	@JsonProperty("email")
	String email
) {
}
