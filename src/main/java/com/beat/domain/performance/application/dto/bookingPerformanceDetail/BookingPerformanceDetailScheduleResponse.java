package com.beat.domain.performance.application.dto.bookingPerformanceDetail;

import java.time.LocalDateTime;

public record BookingPerformanceDetailScheduleResponse(
        Long scheduleId,
        LocalDateTime performanceDate,
        String scheduleNumber,
        int availableTicketCount,
        boolean isBooking,
        int dueDate
) {
    public static BookingPerformanceDetailScheduleResponse of(Long scheduleId, LocalDateTime performanceDate, String scheduleNumber, int availableTicketCount, boolean isBooking, int dueDate) {
        return new BookingPerformanceDetailScheduleResponse(scheduleId, performanceDate, scheduleNumber, availableTicketCount, isBooking, dueDate);
    }

}
