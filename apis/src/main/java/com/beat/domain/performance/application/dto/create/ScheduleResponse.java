package com.beat.domain.performance.application.dto.create;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleResponse(
	Long scheduleId,
	LocalDateTime performanceDate,
	int totalTicketCount,
	int dueDate,
	ScheduleNumber scheduleNumber
) {
	public static ScheduleResponse of(
		Long scheduleId,
		LocalDateTime performanceDate,
		int totalTicketCount,
		int dueDate,
		ScheduleNumber scheduleNumber
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