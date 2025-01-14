package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;

public record BookingRefundResponse(
	long bookingId,
	BookingStatus bookingStatus,
	BankName bankName,
	String accountNumber,
	String accountHolder
) {
	public static BookingRefundResponse of(
		long bookingId,
		BookingStatus bookingStatus,
		BankName bankName,
		String accountNumber,
		String accountHolder) {
		return new BookingRefundResponse(
			bookingId,
			bookingStatus,
			bankName,
			accountNumber,
			accountHolder);
	}
}
