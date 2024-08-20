package com.beat.domain.performance.application.dto.update;

public record ScheduleDeleteResponse(
        Long scheduleId
) {
    public static ScheduleDeleteResponse from(Long scheduleId) {
        return new ScheduleDeleteResponse(scheduleId);
    }
}