package com.beat.apis.booking.application.dto;


public record BookingCancelResponse(
	long bookingId,
	BookingStatusType bookingStatus
) {
	public static BookingCancelResponse of(
		long bookingId,
		BookingStatusType bookingStatus) {
		return new BookingCancelResponse(
			bookingId,
			bookingStatus);
	}
}
