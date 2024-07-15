package com.beat.domain.booking.application.dto;

public record MemberBookingRequest(
        Long scheduleId,
        String scheduleNumber,
        int purchaseTicketCount,
        String bookerName,
        String bookerPhoneNumber,
        boolean isPaymentCompleted,
        int totalPaymentAmount
) {
    public static MemberBookingRequest of(
            Long scheduleId,
            String scheduleNumber,
            int purchaseTicketCount,
            String bookerName,
            String bookerPhoneNumber,
            boolean isPaymentCompleted,
            int totalPaymentAmount
    ) {
        return new MemberBookingRequest(
                scheduleId,
                scheduleNumber,
                purchaseTicketCount,
                bookerName,
                bookerPhoneNumber,
                isPaymentCompleted,
                totalPaymentAmount
        );
    }
}