package com.beat.apis.booking.application.dto;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;

public record MemberBookingRequest(
	Long scheduleId,
	ScheduleNumberType scheduleNumber,
	int purchaseTicketCount,
	String bookerName,
	String bookerPhoneNumber,
	BookingStatusType bookingStatus,
	int totalPaymentAmount
) {
}
