package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

public record MemberBookingRequest(
        Long scheduleId,
        ScheduleNumber scheduleNumber,
        int purchaseTicketCount,
        String bookerName,
        String bookerPhoneNumber,
        BookingStatus bookingStatus,
        int totalPaymentAmount
) { }