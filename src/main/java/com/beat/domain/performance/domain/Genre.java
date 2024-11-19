package com.beat.domain.performance.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Genre {
	BAND("밴드"),
	PLAY("연극/뮤지컬"),
	DANCE("댄스"),
	ETC("기타");

	private final String displayName;
}