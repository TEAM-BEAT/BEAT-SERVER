package com.beat.contracts.booking.readmodel;

import java.time.LocalDateTime;

import com.beat.contracts.common.ReadModel;

@ReadModel
public record MakerTicketListItemReadModel(
	Long bookingId,
	String bookerName,
	String bookerPhoneNumber,
	Long scheduleId,
	int purchaseTicketCount,
	LocalDateTime createdAt,
	MakerTicketBookingStatus bookingStatus,
	String bankName,
	String accountNumber,
	String accountHolder
) {
}
