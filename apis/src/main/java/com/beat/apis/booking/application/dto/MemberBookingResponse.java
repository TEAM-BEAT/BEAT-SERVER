package com.beat.apis.booking.application.dto;

import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;

import java.time.LocalDateTime;

public record MemberBookingResponse(
	Long bookingId,
	Long scheduleId,
	Long userId,
	int purchaseTicketCount,
	ScheduleNumberType scheduleNumber,
	String bookerName,
	String bookerPhoneNumber,
	BookingStatusType bookingStatus,
	BankNameType bankName,
	String accountNumber,
	int totalPaymentAmount,
	LocalDateTime createdAt
) {
	public static MemberBookingResponse of(
		Long bookingId,
		Long scheduleId,
		Long userId,
		int purchaseTicketCount,
		ScheduleNumberType scheduleNumber,
		String bookerName,
		String bookerPhoneNumber,
		BookingStatusType bookingStatus,
		BankNameType bankName,
		String accountNumber,
		int totalPaymentAmount,
		LocalDateTime createdAt
	) {
		return new MemberBookingResponse(
			bookingId,
			scheduleId,
			userId,
			purchaseTicketCount,
			scheduleNumber,
			bookerName,
			bookerPhoneNumber,
			bookingStatus,
			bankName,
			accountNumber,
			totalPaymentAmount,
			createdAt
		);
	}
}
