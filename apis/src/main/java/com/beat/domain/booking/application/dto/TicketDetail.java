package com.beat.domain.booking.application.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;

public record TicketDetail(
	Long bookingId,
	String bookerName,
	String bookerPhoneNumber,
	Long scheduleId,
	int purchaseTicketCount,
	LocalDateTime createdAt,
	BookingStatus bookingStatus,
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
		BookingStatus bookingStatus,
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
			Objects.requireNonNullElse(bankName, BankName.NONE.getDisplayName()),
			Objects.requireNonNullElse(accountNumber, ""),
			Objects.requireNonNullElse(accountHolder, "")
		);
	}
}
