package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

public record GuestBookingRequest(
        Long scheduleId,
        int purchaseTicketCount,
        ScheduleNumber scheduleNumber,
        String bookerName,
        String bookerPhoneNumber,
        String birthDate,
        String password,
        int totalPaymentAmount,
        BookingStatus bookingStatus
) {
    public static GuestBookingRequest of(Long scheduleId, int purchaseTicketCount, ScheduleNumber scheduleNumber, String bookerName, String bookerPhoneNumber, String birthDate, String password, int totalPaymentAmount, BookingStatus bookingStatus) {
        return new GuestBookingRequest(scheduleId, purchaseTicketCount, scheduleNumber, bookerName, bookerPhoneNumber, birthDate, password, totalPaymentAmount, bookingStatus);
    }
}