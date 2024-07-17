package com.beat.domain.performance.application.dto.update;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleUpdateResponse(
        Long scheduleId,
        LocalDateTime performanceDate,
        int totalTicketCount,
        int dueDate,
        ScheduleNumber scheduleNumber
) {
    public static ScheduleUpdateResponse of(Long scheduleId, LocalDateTime performanceDate, int totalTicketCount, int dueDate, ScheduleNumber scheduleNumber) {
        return new ScheduleUpdateResponse(scheduleId, performanceDate, totalTicketCount, dueDate, scheduleNumber);
    }
}
