package com.beat.domain.performance.application.dto;

import java.util.List;

public record BookingPerformanceDetailResponse(
        Long performanceId,
        String performanceTitle,
        String performancePeriod,
        List<BookingPerformanceDetailSchedule> scheduleList,
        int ticketPrice,
        String genre,
        String posterImage,
        String performanceVenue,
        String performanceTeamName
) {
    public static BookingPerformanceDetailResponse of(
            Long performanceId,
            String performanceTitle,
            String performancePeriod,
            List<BookingPerformanceDetailSchedule> scheduleList,
            int ticketPrice,
            String genre,
            String posterImage,
            String performanceVenue,
            String performanceTeamName
    ) {
        return new BookingPerformanceDetailResponse(performanceId, performanceTitle, performancePeriod, scheduleList, ticketPrice, genre, posterImage, performanceVenue, performanceTeamName);
    }

}