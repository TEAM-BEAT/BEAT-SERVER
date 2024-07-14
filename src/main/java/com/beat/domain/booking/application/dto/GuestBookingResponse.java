package com.beat.domain.booking.application.dto;

import java.time.LocalDateTime;

public record GuestBookingResponse(
        Long bookingId,
        Long scheduleId,
        Long userId,
        int purchaseTicketCount,
        String scheduleNumber,
        String bookerName,
        String bookerPhoneNumber,
        boolean isPaymentCompleted,
        String bankName,
        String accountNumber,
        int totalPaymentAmount,
        LocalDateTime createdAt
) {
    public static GuestBookingResponse of(
            Long bookingId,
            Long scheduleId,
            Long userId,
            int purchaseTicketCount,
            String scheduleNumber,
            String bookerName,
            String bookerPhoneNumber,
            boolean isPaymentCompleted,
            String bankName,
            String accountNumber,
            int totalPaymentAmount,
            LocalDateTime createdAt) {
        return new GuestBookingResponse(
                bookingId,
                scheduleId,
                userId,
                purchaseTicketCount,
                scheduleNumber,
                bookerName,
                bookerPhoneNumber,
                isPaymentCompleted,
                bankName,
                accountNumber,
                totalPaymentAmount,
                createdAt);
    }
}