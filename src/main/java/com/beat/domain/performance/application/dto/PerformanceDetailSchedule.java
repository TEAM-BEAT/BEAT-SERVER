package com.beat.domain.performance.application.dto;

import java.time.LocalDateTime;

public record PerformanceDetailSchedule(
        Long scheduleId,
        LocalDateTime performanceDate,
        String scheduleNumber
) {
    public static PerformanceDetailSchedule of(Long scheduleId, LocalDateTime performanceDate, String scheduleNumber) {
        return new PerformanceDetailSchedule(scheduleId, performanceDate, scheduleNumber);
    }
}
