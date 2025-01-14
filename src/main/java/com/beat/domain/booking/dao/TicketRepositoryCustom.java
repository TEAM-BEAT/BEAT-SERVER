package com.beat.domain.booking.dao;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

import java.util.List;

public interface TicketRepositoryCustom {

	List<Booking> findBookingsByPerformanceIdAndScheduleNumbersAndBookingStatuses(
		Long performanceId,
		List<ScheduleNumber> scheduleNumbers,
		List<BookingStatus> bookingStatuses
	);

	List<Booking> searchBookingsByPerformanceIdAndSearchWordAndSchedulesNumbersAndBookingStatuses(
		Long performanceId,
		String searchWord,
		List<String> selectedScheduleNumbers,
		List<String> selectedBookingStatuses
	);
}
