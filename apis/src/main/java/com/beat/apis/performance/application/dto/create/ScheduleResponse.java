package com.beat.apis.performance.application.dto.create;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;

import java.time.LocalDateTime;

public record ScheduleResponse(
	Long scheduleId,
	LocalDateTime performanceDate,
	int totalTicketCount,
	int dueDate,
	ScheduleNumberType scheduleNumber
) {
	public static ScheduleResponse of(
		Long scheduleId,
		LocalDateTime performanceDate,
		int totalTicketCount,
		int dueDate,
		ScheduleNumberType scheduleNumber
	) {
		return new ScheduleResponse(
			scheduleId,
			performanceDate,
			totalTicketCount,
			dueDate,
			scheduleNumber
		);
	}
}
