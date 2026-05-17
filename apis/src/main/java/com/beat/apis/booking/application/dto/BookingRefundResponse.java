package com.beat.apis.booking.application.dto;

import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.performance.application.dto.BankNameType;

public record BookingRefundResponse(
	long bookingId,
	BookingStatusType bookingStatus,
	BankNameType bankName,
	String accountNumber,
	String accountHolder
) {
	public static BookingRefundResponse of(
		long bookingId,
		BookingStatusType bookingStatus,
		BankNameType bankName,
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
