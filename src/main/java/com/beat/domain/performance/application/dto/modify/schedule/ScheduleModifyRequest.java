package com.beat.domain.performance.application.dto.modify.schedule;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public record ScheduleModifyRequest(
        @Nullable
        Long scheduleId,
        LocalDateTime performanceDate,
        int totalTicketCount
) {
}