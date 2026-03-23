package com.beat.domain.schedule.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleNumber {
	FIRST("1회차"),
	SECOND("2회차"),
	THIRD("3회차"),
	FOURTH("4회차"),
	FIFTH("5회차"),
	SIXTH("6회차"),
	SEVENTH("7회차"),
	EIGHTH("8회차"),
	NINTH("9회차"),
	TENTH("10회차");

	private final String displayName;
}
