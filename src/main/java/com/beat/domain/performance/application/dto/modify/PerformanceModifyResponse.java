package com.beat.domain.performance.application.dto.modify;

import com.beat.domain.performance.application.dto.modify.cast.CastModifyResponse;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyResponse;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyResponse;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import java.util.List;

public record PerformanceModifyResponse(
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
        List<ScheduleModifyResponse> scheduleModifyResponses,
        List<CastModifyResponse> castModifyResponses,
        List<StaffModifyResponse> staffModifyResponses
) {
    public static PerformanceModifyResponse of(
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
            List<ScheduleModifyResponse> scheduleModifyRespons,
            List<CastModifyResponse> castModifyRespons,
            List<StaffModifyResponse> staffModifyRespons)
            {

        return new PerformanceModifyResponse(
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
                scheduleModifyRespons,
                castModifyRespons,
                staffModifyRespons
        );
    }
}