package com.beat.domain.booking.application.dto;

import java.time.LocalDateTime;

public record TicketUpdateDetail(
        Long bookingId,
        String bookerName,
        String bookerPhoneNumber,
        Long scheduleId,
        int purchaseTicketCount,
        LocalDateTime createdAt,
        boolean isPaymentCompleted,
        String scheduleNumber
) {
    public static TicketUpdateDetail of(
            Long bookingId,
            String bookerName,
            String bookerPhoneNumber,
            Long scheduleId,
            int purchaseTicketCount,
            LocalDateTime createdAt,
            boolean isPaymentCompleted,
            String scheduleNumber) {
        return new TicketUpdateDetail(bookingId, bookerName, bookerPhoneNumber, scheduleId, purchaseTicketCount, createdAt, isPaymentCompleted, scheduleNumber);
    }
}
