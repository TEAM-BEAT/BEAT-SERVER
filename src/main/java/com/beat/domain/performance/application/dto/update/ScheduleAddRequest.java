package com.beat.domain.performance.application.dto.update;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleAddRequest(
        LocalDateTime performanceDate,
        int totalTicketCount,
        ScheduleNumber scheduleNumber
) {}