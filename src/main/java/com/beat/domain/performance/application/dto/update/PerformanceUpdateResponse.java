package com.beat.domain.performance.application.dto.update;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import java.util.List;

public record PerformanceUpdateResponse(
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
        List<ScheduleAddResponse> addedSchedules,
        List<ScheduleDeleteResponse> deletedSchedules,
        List<ScheduleUpdateResponse> updatedSchedules,
        List<CastAddResponse> addedCasts,
        List<CastDeleteResponse> deletedCasts,
        List<CastUpdateResponse> updatedCasts,
        List<StaffAddResponse> addedStaffs,
        List<StaffDeleteResponse> deletedStaffs,
        List<StaffUpdateResponse> updatedStaffs
) {
    public static PerformanceUpdateResponse of(
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
            List<ScheduleAddResponse> addedSchedules,
            List<ScheduleDeleteResponse> deletedSchedules,
            List<ScheduleUpdateResponse> updatedSchedules,
            List<CastAddResponse> addedCasts,
            List<CastDeleteResponse> deletedCasts,
            List<CastUpdateResponse> updatedCasts,
            List<StaffAddResponse> addedStaffs,
            List<StaffDeleteResponse> deletedStaffs,
            List<StaffUpdateResponse> updatedStaffs) {

        return new PerformanceUpdateResponse(
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
                addedSchedules,
                deletedSchedules,
                updatedSchedules,
                addedCasts,
                deletedCasts,
                updatedCasts,
                addedStaffs,
                deletedStaffs,
                updatedStaffs
        );
    }
}