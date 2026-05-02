package com.beat.apis.booking.application.dto;

import com.beat.apis.performance.application.dto.BankNameType;

public record BookingRefundRequest(
	long bookingId,
	BankNameType bankName,
	String accountNumber,
	String accountHolder
) {
}
