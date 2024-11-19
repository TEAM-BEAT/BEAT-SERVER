package com.beat.domain.performance.application.dto.performanceDetail;

import java.time.LocalDateTime;

public record PerformanceDetailScheduleResponse(
	Long scheduleId,
	LocalDateTime performanceDate,
	String scheduleNumber,
	int dueDate,
	boolean isBooking
) {
	public static PerformanceDetailScheduleResponse of(Long scheduleId, LocalDateTime performanceDate,
		String scheduleNumber, int dueDate, boolean isBooking) {
		return new PerformanceDetailScheduleResponse(scheduleId, performanceDate, scheduleNumber, dueDate, isBooking);
	}
}
