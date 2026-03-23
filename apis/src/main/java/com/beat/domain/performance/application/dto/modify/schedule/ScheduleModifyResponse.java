package com.beat.domain.performance.application.dto.modify.schedule;

import java.time.LocalDateTime;

import com.beat.domain.schedule.domain.ScheduleNumber;

public record ScheduleModifyResponse(
	Long scheduleId,
	LocalDateTime performanceDate,
	int totalTicketCount,
	int dueDate,
	ScheduleNumber scheduleNumber
) {
	public static ScheduleModifyResponse of(Long scheduleId, LocalDateTime performanceDate, int totalTicketCount,
		int dueDate, ScheduleNumber scheduleNumber) {
		return new ScheduleModifyResponse(scheduleId, performanceDate, totalTicketCount, dueDate, scheduleNumber);
	}
}
