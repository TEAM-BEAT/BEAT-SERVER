package com.beat.domain.booking.application.dto;

import java.time.LocalDateTime;

public record TicketDetail(
        Long bookingId,
        String bookerName,
        String bookerPhoneNumber,
        Long scheduleId,
        int purchaseTicketCount,
        LocalDateTime createdAt,
        boolean isPaymentCompleted,
        String scheduleNumber
) {
    public static TicketDetail of(
            Long bookingId,
            String bookerName,
            String bookerPhoneNumber,
            Long scheduleId,
            int purchaseTicketCount,
            LocalDateTime createdAt,
            boolean isPaymentCompleted,
            String scheduleNumber) {
        return new TicketDetail(bookingId, bookerName, bookerPhoneNumber, scheduleId, purchaseTicketCount, createdAt, isPaymentCompleted, scheduleNumber);
    }
}
