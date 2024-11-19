package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record MemberBookingRetrieveResponse(
	Long userId,
	Long bookingId,
	Long scheduleId,
	Long performanceId,
	String performanceTitle,
	LocalDateTime performanceDate,
	String performanceVenue,
	int purchaseTicketCount,
	ScheduleNumber scheduleNumber,
	String bookerName,
	String performanceContact,
	BankName bankName,
	String accountNumber,
	String accountHolder,
	int dueDate,
	BookingStatus bookingStatus,
	LocalDateTime createdAt,
	String posterImage,
	int totalPaymentAmount
) {
	public static MemberBookingRetrieveResponse of(
		Long userId,
		Long bookingId,
		Long scheduleId,
		Long performanceId,
		String performanceTitle,
		LocalDateTime performanceDate,
		String performanceVenue,
		int purchaseTicketCount,
		ScheduleNumber scheduleNumber,
		String bookerName,
		String performanceContact,
		BankName bankName,
		String accountNumber,
		String accountHolder,
		int dueDate,
		BookingStatus bookingStatus,
		LocalDateTime createdAt,
		String posterImage,
		int totalPaymentAmount
	) {
		return new MemberBookingRetrieveResponse(
			userId,
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