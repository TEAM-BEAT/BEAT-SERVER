package com.beat.apis.booking.application.dto;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;

public record GuestBookingRequest(
	Long scheduleId,
	int purchaseTicketCount,
	ScheduleNumberType scheduleNumber,
	String bookerName,
	String bookerPhoneNumber,
	String birthDate,
	String password,
	int totalPaymentAmount,
	BookingStatusType bookingStatus
) {
	public static GuestBookingRequest of(Long scheduleId, int purchaseTicketCount, ScheduleNumberType scheduleNumber,
		String bookerName, String bookerPhoneNumber, String birthDate, String password, int totalPaymentAmount,
		BookingStatusType bookingStatus) {
		return new GuestBookingRequest(scheduleId, purchaseTicketCount, scheduleNumber, bookerName, bookerPhoneNumber,
			birthDate, password, totalPaymentAmount, bookingStatus);
	}
}
