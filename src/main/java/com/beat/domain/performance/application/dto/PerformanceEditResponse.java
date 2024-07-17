package com.beat.domain.performance.application.dto;

import com.beat.domain.performance.application.dto.create.CastResponse;
import com.beat.domain.performance.application.dto.create.ScheduleResponse;
import com.beat.domain.performance.application.dto.create.StaffResponse;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import java.util.List;

public record PerformanceEditResponse(
        Long userId,
        Long performanceId,
        String performanceTitle,
        Genre genre,
        int runningTime,
        String performanceDescription,
        String performanceAttentionNote,
        BankName bankName,
        String accountNumber,
        String accountHolder,
        String posterImage,
        String performanceTeamName,
        String performanceVenue,
        String performanceContact,
        String performancePeriod,
        int ticketPrice,
        int totalScheduleCount,
        boolean isBookerExist,
        List<ScheduleResponse> scheduleList,
        List<CastResponse> castList,
        List<StaffResponse> staffList
) {
    public static PerformanceEditResponse of(
            Long userId,
            Long performanceId,
            String performanceTitle,
            Genre genre,
            int runningTime,
            String performanceDescription,
            String performanceAttentionNote,
            BankName bankName,
            String accountNumber,
            String accountHolder,
            String posterImage,
            String performanceTeamName,
            String performanceVenue,
            String performanceContact,
            String performancePeriod,
            int ticketPrice,
            int totalScheduleCount,
            boolean isBookerExist,
            List<ScheduleResponse> scheduleList,
            List<CastResponse> castList,
            List<StaffResponse> staffList
    ) {
        return new PerformanceEditResponse(
                userId,
                performanceId,
                performanceTitle,
                genre,
                runningTime,
                performanceDescription,
                performanceAttentionNote,
                bankName,
                accountNumber,
                accountHolder,
                posterImage,
                performanceTeamName,
                performanceVenue,
                performanceContact,
                performancePeriod,
                ticketPrice,
                totalScheduleCount,
                isBookerExist,
                scheduleList,
                castList,
                staffList
        );
    }
}