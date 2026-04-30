package com.beat.batch.booking.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCleanupScheduler {

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	private final BookingRepository bookingRepository;

	@Scheduled(cron = "0 0 4 * * ?")
	@Transactional
	public void deleteOldCancelledBookings() {
		if (!schedulerOwner) {
			return;
		}

		LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
		List<Booking> oldCancelledBookings = bookingRepository.findByBookingStatusAndCancellationDateBefore(
			BookingStatus.BOOKING_CANCELLED, oneYearAgo);
		bookingRepository.deleteAll(oldCancelledBookings);
	}
}
