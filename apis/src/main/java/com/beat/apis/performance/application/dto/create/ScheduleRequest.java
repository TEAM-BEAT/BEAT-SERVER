package com.beat.apis.performance.application.dto.create;

import java.time.LocalDateTime;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;

public record ScheduleRequest(
	LocalDateTime performanceDate,
	int totalTicketCount,
	ScheduleNumberType scheduleNumber
) {
}
