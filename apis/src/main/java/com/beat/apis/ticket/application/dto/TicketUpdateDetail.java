package com.beat.apis.ticket.application.dto;

import com.beat.apis.booking.application.dto.BookingStatusType;

import java.time.LocalDateTime;

public record TicketUpdateDetail(
	Long bookingId,
	String bookerName,
	String bookerPhoneNumber,
	Long scheduleId,
	int purchaseTicketCount,
	LocalDateTime createdAt,
	BookingStatusType bookingStatus,
	String scheduleNumber
) {
	public static TicketUpdateDetail of(
		Long bookingId,
		String bookerName,
		String bookerPhoneNumber,
		Long scheduleId,
		int purchaseTicketCount,
		LocalDateTime createdAt,
		BookingStatusType bookingStatus,
		String scheduleNumber) {
		return new TicketUpdateDetail(bookingId, bookerName, bookerPhoneNumber, scheduleId, purchaseTicketCount,
			createdAt, bookingStatus, scheduleNumber);
	}
}
