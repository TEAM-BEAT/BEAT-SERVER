package com.beat.apis.ticket.application.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import com.beat.apis.booking.application.dto.BookingStatusType;

public record TicketDetail(
	Long bookingId,
	String bookerName,
	String bookerPhoneNumber,
	Long scheduleId,
	int purchaseTicketCount,
	LocalDateTime createdAt,
	BookingStatusType bookingStatus,
	String scheduleNumber,
	String bankName,
	String accountNumber,
	String accountHolder
) {
	public static TicketDetail of(
		Long bookingId,
		String bookerName,
		String bookerPhoneNumber,
		Long scheduleId,
		int purchaseTicketCount,
		LocalDateTime createdAt,
		BookingStatusType bookingStatus,
		String scheduleNumber,
		String bankName,
		String accountNumber,
		String accountHolder) {
		return new TicketDetail(
			bookingId,
			bookerName,
			bookerPhoneNumber,
			scheduleId,
			purchaseTicketCount,
			createdAt,
			bookingStatus,
			scheduleNumber,
			Objects.requireNonNullElse(bankName, ""),
			Objects.requireNonNullElse(accountNumber, ""),
			Objects.requireNonNullElse(accountHolder, "")
		);
	}
}
