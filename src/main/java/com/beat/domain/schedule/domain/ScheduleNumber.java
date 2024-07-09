package com.beat.domain.schedule.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleNumber {
    FIRST("첫번째"),
    SECOND("두번째"),
    THIRD("세번째");

    private final String displayName;
}
