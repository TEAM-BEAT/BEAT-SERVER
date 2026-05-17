package com.beat.apis.performance.application.dto.modify.schedule;

import java.time.LocalDateTime;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;

public record ScheduleModifyResponse(
	Long scheduleId,
	LocalDateTime performanceDate,
	int totalTicketCount,
	int dueDate,
	ScheduleNumberType scheduleNumber
) {
	public static ScheduleModifyResponse of(Long scheduleId, LocalDateTime performanceDate, int totalTicketCount,
		int dueDate, ScheduleNumberType scheduleNumber) {
		return new ScheduleModifyResponse(scheduleId, performanceDate, totalTicketCount, dueDate, scheduleNumber);
	}
}
