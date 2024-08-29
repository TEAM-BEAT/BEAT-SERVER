package com.beat.domain.booking.application.dto;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record MemberBookingResponse(
        Long bookingId,
        Long scheduleId,
        Long userId,
        int purchaseTicketCount,
        ScheduleNumber scheduleNumber,
        String bookerName,
        String bookerPhoneNumber,
        BookingStatus bookingStatus,
        BankName bankName,
        String accountNumber,
        int totalPaymentAmount,
        LocalDateTime createdAt
) {
    public static MemberBookingResponse of(
            Long bookingId,
            Long scheduleId,
            Long userId,
            int purchaseTicketCount,
            ScheduleNumber scheduleNumber,
            String bookerName,
            String bookerPhoneNumber,
            BookingStatus bookingStatus,
            BankName bankName,
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
                bookingStatus,
                bankName,
                accountNumber,
                totalPaymentAmount,
                createdAt
        );
    }
}