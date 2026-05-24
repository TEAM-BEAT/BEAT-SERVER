package com.beat.apis.booking.application.dto;

import java.time.LocalDateTime;

import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.global.support.jackson.CdnImageUrl;

public record GuestBookingRetrieveResponse(
	Long bookingId,
	Long scheduleId,
	Long performanceId,
	String performanceTitle,
	LocalDateTime performanceDate,
	String performanceVenue,
	int purchaseTicketCount,
	ScheduleNumberType scheduleNumber,
	String bookerName,
	String performanceContact,
	BankNameType bankName,
	String accountNumber,
	String accountHolder,
	int dueDate,
	BookingStatusType bookingStatus,
	LocalDateTime createdAt,
	@CdnImageUrl String posterImage,
	int totalPaymentAmount
) {
	public static GuestBookingRetrieveResponse of(
		Long bookingId,
		Long scheduleId,
		Long performanceId,
		String performanceTitle,
		LocalDateTime performanceDate,
		String performanceVenue,
		int purchaseTicketCount,
		ScheduleNumberType scheduleNumber,
		String bookerName,
		String performanceContact,
		BankNameType bankName,
		String accountNumber,
		String accountHolder,
		int dueDate,
		BookingStatusType bookingStatus,
		LocalDateTime createdAt,
		String posterImage,
		int totalPaymentAmount
	) {
		return new GuestBookingRetrieveResponse(
			bookingId,
			scheduleId,
			performanceId,
			performanceTitle,
			performanceDate,
			performanceVenue,
			purchaseTicketCount,
			scheduleNumber,
			bookerName,
			performanceContact,
			bankName,
			accountNumber,
			accountHolder,
			dueDate,
			bookingStatus,
			createdAt,
			posterImage,
			totalPaymentAmount
		);
	}
}
