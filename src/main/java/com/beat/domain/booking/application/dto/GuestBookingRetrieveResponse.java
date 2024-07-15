package com.beat.domain.booking.application.dto;

import java.time.LocalDateTime;

public record GuestBookingRetrieveResponse(
        Long bookingId,
        Long scheduleId,
        String performanceTitle,
        LocalDateTime performanceDate,
        String performanceVenue,
        int purchaseTicketCount,
        String scheduleNumber,
        String bookerName,
        String bookerPhoneNumber,
        String bankName,
        String accountNumber,
        int dueDate,
        boolean isPaymentCompleted,
        LocalDateTime createdAt
) {
    public static GuestBookingRetrieveResponse of(
            Long bookingId,
            Long scheduleId,
            String performanceTitle,
            LocalDateTime performanceDate,
            String performanceVenue,
            int purchaseTicketCount,
            String scheduleNumber,
            String bookerName,
            String bookerPhoneNumber,
            String bankName,
            String accountNumber,
            int dueDate,
            boolean isPaymentCompleted,
            LocalDateTime createdAt
    ) {
        return new GuestBookingRetrieveResponse(
                bookingId,
                scheduleId,
                performanceTitle,
                performanceDate,
                performanceVenue,
                purchaseTicketCount,
                scheduleNumber,
                bookerName,
                bookerPhoneNumber,
                bankName,
                accountNumber,
                dueDate,
                isPaymentCompleted,
                createdAt
        );
    }
}