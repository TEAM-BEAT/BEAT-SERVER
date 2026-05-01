package com.beat.batch.booking.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCleanupScheduler {

	private final BookingRepository bookingRepository;

	@Transactional
	public void deleteOldCancelledBookings() {
		LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
		List<Booking> oldCancelledBookings = bookingRepository.findByBookingStatusAndCancellationDateBefore(
			BookingStatus.BOOKING_CANCELLED, oneYearAgo);
		bookingRepository.deleteAll(oldCancelledBookings);
	}
}
