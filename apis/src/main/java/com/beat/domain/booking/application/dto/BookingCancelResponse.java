package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;

public record BookingCancelResponse(
	long bookingId,
	BookingStatus bookingStatus
) {
	public static BookingCancelResponse of(
		long bookingId,
		BookingStatus bookingStatus) {
		return new BookingCancelResponse(
			bookingId,
			bookingStatus);
	}
}
