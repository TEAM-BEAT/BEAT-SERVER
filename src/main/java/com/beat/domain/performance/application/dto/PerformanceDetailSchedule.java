package com.beat.domain.performance.application.dto;

import java.time.LocalDateTime;

public record PerformanceDetailSchedule(
        Long scheduleId,
        LocalDateTime performanceDate,
        String scheduleNumber,
        int dueDate,
        boolean isBooking
) {
    public static PerformanceDetailSchedule of(Long scheduleId, LocalDateTime performanceDate, String scheduleNumber, int dueDate, boolean isBooking) {
        return new PerformanceDetailSchedule(scheduleId, performanceDate, scheduleNumber, dueDate, isBooking);
    }
}
