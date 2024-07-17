package com.beat.domain.performance.application.dto.create;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleRequest(
        LocalDateTime performanceDate,
        int totalTicketCount,
        ScheduleNumber scheduleNumber
) {}