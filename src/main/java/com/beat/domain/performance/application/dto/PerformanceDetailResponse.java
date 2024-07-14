package com.beat.domain.performance.application.dto;

import java.util.List;

public record PerformanceDetailResponse(
        Long performanceId,
        String performanceTitle,
        String performancePeriod,
        List<PerformanceDetailSchedule> scheduleList,
        int ticketPrice,
        String genre,
        String posterImage,
        int runningTime,
        String performanceVenue,
        String performanceDescription,
        String performanceAttentionNote,
        String performanceContact,
        String performanceTeamName,
        List<PerformanceDetailCast> castList,
        List<PerformanceDetailStaff> staffList
) {
    public static PerformanceDetailResponse of(
            Long performanceId,
            String performanceTitle,
            String performancePeriod,
            List<PerformanceDetailSchedule> scheduleList,
            int ticketPrice,
            String genre,
            String posterImage,
            int runningTime,
            String performanceVenue,
            String performanceDescription,
            String performanceAttentionNote,
            String performanceContact,
            String performanceTeamName,
            List<PerformanceDetailCast> castList,
            List<PerformanceDetailStaff> staffList
    ) {
        return new PerformanceDetailResponse(performanceId, performanceTitle, performancePeriod, scheduleList, ticketPrice, genre, posterImage, runningTime, performanceVenue, performanceDescription, performanceAttentionNote, performanceContact, performanceTeamName, castList, staffList);
    }


}
