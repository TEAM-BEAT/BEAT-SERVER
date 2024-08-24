package com.beat.domain.performance.application.dto.performanceDetail;

import java.util.List;

public record PerformanceDetailResponse(
        Long performanceId,
        String performanceTitle,
        String performancePeriod,
        List<PerformanceDetailScheduleResponse> scheduleList,
        int ticketPrice,
        String genre,
        String posterImage,
        int runningTime,
        String performanceVenue,
        String performanceDescription,
        String performanceAttentionNote,
        String performanceContact,
        String performanceTeamName,
        List<PerformanceDetailCastResponse> castList,
        List<PerformanceDetailStaffResponse> staffList,
        int minDueDate,
        List<PerformanceDetailImageResponse> performanceImageList
) {
    public static PerformanceDetailResponse of(
            Long performanceId,
            String performanceTitle,
            String performancePeriod,
            List<PerformanceDetailScheduleResponse> scheduleList,
            int ticketPrice,
            String genre,
            String posterImage,
            int runningTime,
            String performanceVenue,
            String performanceDescription,
            String performanceAttentionNote,
            String performanceContact,
            String performanceTeamName,
            List<PerformanceDetailCastResponse> castList,
            List<PerformanceDetailStaffResponse> staffList,
            int minDueDate,
            List<PerformanceDetailImageResponse> performanceImageList
    ) {
        return new PerformanceDetailResponse(
                performanceId,
                performanceTitle,
                performancePeriod,
                scheduleList,
                ticketPrice,
                genre,
                posterImage,
                runningTime,
                performanceVenue,
                performanceDescription,
                performanceAttentionNote,
                performanceContact,
                performanceTeamName,
                castList,
                staffList,
                minDueDate,
                performanceImageList
        );
    }
}
