package com.beat.apis.performance.application.dto.create;

import java.time.LocalDateTime;

import com.beat.domain.schedule.domain.ScheduleNumber;

public record ScheduleRequest(
	LocalDateTime performanceDate,
	int totalTicketCount,
	ScheduleNumber scheduleNumber
) {
}
