package com.beat.domain.performance.application.dto.update.schedule;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleAddRequest(
        LocalDateTime performanceDate,
        int totalTicketCount,
        ScheduleNumber scheduleNumber
) {}