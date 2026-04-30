package com.beat.domain.member.domain;

public enum SocialType {
	KAKAO("KAKAO"),
	;
	private final String type;

	SocialType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}