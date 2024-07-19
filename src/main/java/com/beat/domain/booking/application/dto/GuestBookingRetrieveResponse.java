package com.beat.domain.booking.application.dto;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.schedule.domain.ScheduleNumber;

import java.time.LocalDateTime;

public record GuestBookingRetrieveResponse(
        Long bookingId,
        Long scheduleId,
        Long performanceId,
        String performanceTitle,
        LocalDateTime performanceDate,
        String performanceVenue,
        int purchaseTicketCount,
        ScheduleNumber scheduleNumber,
        String bookerName,
        String performanceContact,
        BankName bankName,
        String accountNumber,
        String accountHolder,
        int dueDate,
        boolean isPaymentCompleted,
        LocalDateTime createdAt,
        String posterImage,
        int totalPaymentAmount
) {
    public static GuestBookingRetrieveResponse of(
            Long bookingId,
            Long scheduleId,
            Long performanceId,
            String performanceTitle,
            LocalDateTime performanceDate,
            String performanceVenue,
            int purchaseTicketCount,
            ScheduleNumber scheduleNumber,
            String bookerName,
            String performanceContact,
            BankName bankName,
            String accountNumber,
            String accountHolder,
            int dueDate,
            boolean isPaymentCompleted,
            LocalDateTime createdAt,
            String posterImage,
            int totalPaymentAmount
    ) {
        return new GuestBookingRetrieveResponse(
                bookingId,
                scheduleId,
                performanceId,
                performanceTitle,
                performanceDate,
                performanceVenue,
                purchaseTicketCount,
                scheduleNumber,
                bookerName,
                performanceContact,
                bankName,
                accountNumber,
                accountHolder,
                dueDate,
                isPaymentCompleted,
                createdAt,
                posterImage,
                totalPaymentAmount
        );
    }
}