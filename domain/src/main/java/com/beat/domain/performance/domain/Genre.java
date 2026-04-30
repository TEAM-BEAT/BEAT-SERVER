package com.beat.domain.performance.domain;

public enum Genre {
	BAND("밴드"),
	PLAY("연극/뮤지컬"),
	DANCE("댄스"),
	ETC("기타");

	private final String displayName;

	Genre(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}