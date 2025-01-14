package com.beat.domain.booking.application.dto;

import com.beat.domain.performance.domain.BankName;

public record BookingRefundRequest(
	long bookingId,
	BankName bankName,
	String accountNumber,
	String accountHolder
) {
}
