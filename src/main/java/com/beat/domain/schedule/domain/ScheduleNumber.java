package com.beat.domain.schedule.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleNumber {
    FIRST("1회차"),
    SECOND("2회차"),
    THIRD("3회차");

    private final String displayName;
}
