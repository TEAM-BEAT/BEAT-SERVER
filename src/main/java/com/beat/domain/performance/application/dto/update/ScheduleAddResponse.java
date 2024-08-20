package com.beat.domain.performance.application.dto.update;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleAddResponse(
        Long scheduleId,
        LocalDateTime performanceDate,
        int totalTicketCount,
        int dueDate,
        ScheduleNumber scheduleNumber
) {
    public static ScheduleAddResponse of(
            Long scheduleId,
            LocalDateTime performanceDate,
            int totalTicketCount,
            int dueDate,
            ScheduleNumber scheduleNumber
    ) {
        return new ScheduleAddResponse(scheduleId, performanceDate, totalTicketCount, dueDate, scheduleNumber);
    }
}