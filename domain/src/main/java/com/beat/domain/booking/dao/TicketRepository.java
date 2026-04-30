package com.beat.domain.booking.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

public interface TicketRepository {

	Optional<Booking> findById(Long id);

	Booking save(Booking booking);

	void deleteAll(Iterable<Booking> bookings);

	List<Booking> findByBookingStatusAndCancellationDateBefore(
		BookingStatus bookingStatus,
		LocalDateTime cancellationDate
	);

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
