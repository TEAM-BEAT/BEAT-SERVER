package com.beat.contracts.schedule;

public interface ScheduleBookingCloseJobPort {

	void registerOrRefresh(ScheduleBookingCloseJobTarget target);

	void cancel(ScheduleBookingCloseJobTarget target);
}
