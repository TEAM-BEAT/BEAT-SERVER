package com.beat.domain.performance.application.dto.update;

import java.time.LocalDateTime;

public record ScheduleUpdateRequest(
        Long scheduleId,
        LocalDateTime performanceDate,
        int totalTicketCount,
        String scheduleNumber
) {}