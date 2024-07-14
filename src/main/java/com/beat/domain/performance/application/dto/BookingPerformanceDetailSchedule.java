package com.beat.domain.performance.application.dto;

import java.time.LocalDateTime;

public record BookingPerformanceDetailSchedule(
        Long scheduleId,
        LocalDateTime performanceDate,
        String scheduleNumber,
        int availableTicketCount,
        boolean isBooking
) {
    public static BookingPerformanceDetailSchedule of(Long scheduleId, LocalDateTime performanceDate, String scheduleNumber, int availableTicketCount, boolean isBooking) {
        return new BookingPerformanceDetailSchedule(scheduleId, performanceDate, scheduleNumber, availableTicketCount, isBooking);
    }

}
