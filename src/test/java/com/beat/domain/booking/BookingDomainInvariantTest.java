package com.beat.domain.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;

class BookingDomainInvariantTest {

	@Test
	void createRejectsNullScheduleId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Booking.create(
			1,
			"booker",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			null,
			null,
			null,
			null,
			1L
		));

		assertEquals("scheduleId must not be null", exception.getMessage());
	}
}
