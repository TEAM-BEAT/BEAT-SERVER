package com.beat.domain.performance.application.dto.modify.schedule;

import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record ScheduleModifyResponse(Long scheduleId,
									 LocalDateTime performanceDate,
									 int totalTicketCount,
									 int dueDate,
									 ScheduleNumber scheduleNumber) {

	public static ScheduleModifyResponse of(Long scheduleId, LocalDateTime performanceDate, int totalTicketCount,
		int dueDate, ScheduleNumber scheduleNumber) {
		return new ScheduleModifyResponse(scheduleId, performanceDate, totalTicketCount, dueDate, scheduleNumber);
	}
}