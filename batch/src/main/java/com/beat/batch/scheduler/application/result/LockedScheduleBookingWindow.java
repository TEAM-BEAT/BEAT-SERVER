package com.beat.batch.scheduler.application.result;

import java.time.LocalDateTime;

public record LockedScheduleBookingWindow(
	Long scheduleId,
	Long performanceId,
	LocalDateTime performanceDate
) {

	public static LockedScheduleBookingWindow of(Long scheduleId, Long performanceId, LocalDateTime performanceDate) {
		return new LockedScheduleBookingWindow(scheduleId, performanceId, performanceDate);
	}
}
