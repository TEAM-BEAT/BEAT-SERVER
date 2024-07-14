package com.beat.domain.booking.application.dto;

public record GuestBookingRequest(
        Long scheduleId,
        int purchaseTicketCount,
        String scheduleNumber,
        String bookerName,
        String bookerPhoneNumber,
        String birthDate,
        String password,
        int totalPaymentAmount,
        boolean isPaymentCompleted
) {
    public static GuestBookingRequest of(Long scheduleId, int purchaseTicketCount, String scheduleNumber, String bookerName, String bookerPhoneNumber, String birthDate, String password, int totalPaymentAmount, boolean isPaymentCompleted) {
        return new GuestBookingRequest(scheduleId, purchaseTicketCount, scheduleNumber, bookerName, bookerPhoneNumber, birthDate, password, totalPaymentAmount, isPaymentCompleted);
    }
}