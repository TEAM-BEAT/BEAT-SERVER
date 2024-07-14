package com.beat.domain.booking.application.dto;

import java.time.LocalDateTime;

public record MemberBookingResponse(
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
    public static MemberBookingResponse of(
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
        return new MemberBookingResponse(
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
                createdAt
        );
    }
}