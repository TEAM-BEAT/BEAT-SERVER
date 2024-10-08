package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;

import java.time.LocalDateTime;

public record TicketDetail(
        Long bookingId,
        String bookerName,
        String bookerPhoneNumber,
        Long scheduleId,
        int purchaseTicketCount,
        LocalDateTime createdAt,
        BookingStatus bookingStatus,
        String scheduleNumber
) {
    public static TicketDetail of(
            Long bookingId,
            String bookerName,
            String bookerPhoneNumber,
            Long scheduleId,
            int purchaseTicketCount,
            LocalDateTime createdAt,
            BookingStatus bookingStatus,
            String scheduleNumber) {
        return new TicketDetail(bookingId, bookerName, bookerPhoneNumber, scheduleId, purchaseTicketCount, createdAt, bookingStatus, scheduleNumber);
    }
}
