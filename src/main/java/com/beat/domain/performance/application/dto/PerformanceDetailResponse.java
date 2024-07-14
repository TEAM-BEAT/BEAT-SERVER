package com.beat.domain.performance.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PerformanceDetailResponse(
        Long performanceId,
        String performanceTitle,
        String performancePeriod,
        List<ScheduleDetail> scheduleList,
        int ticketPrice,
        String genre,
        String posterImage,
        int runningTime,
        String performanceVenue,
        String performanceDescription,
        String performanceAttentionNote,
        String performanceContact,
        String performanceTeamName,
        List<CastDetail> castList,
        List<StaffDetail> staffList
) {
    public static PerformanceDetailResponse of(
            Long performanceId,
            String performanceTitle,
            String performancePeriod,
            List<ScheduleDetail> scheduleList,
            int ticketPrice,
            String genre,
            String posterImage,
            int runningTime,
            String performanceVenue,
            String performanceDescription,
            String performanceAttentionNote,
            String performanceContact,
            String performanceTeamName,
            List<CastDetail> castList,
            List<StaffDetail> staffList
    ) {
        return new PerformanceDetailResponse(performanceId, performanceTitle, performancePeriod, scheduleList, ticketPrice, genre, posterImage, runningTime, performanceVenue, performanceDescription, performanceAttentionNote, performanceContact, performanceTeamName, castList, staffList);
    }

    public record ScheduleDetail(
            Long scheduleId,
            LocalDateTime performanceDate,
            String scheduleNumber
    ) {
        public static ScheduleDetail of(Long scheduleId, LocalDateTime performanceDate, String scheduleNumber) {
            return new ScheduleDetail(scheduleId, performanceDate, scheduleNumber);
        }
    }

    public record CastDetail(
            Long castId,
            String castName,
            String castRole,
            String castPhoto
    ) {
        public static CastDetail of(Long castId, String castName, String castRole, String castPhoto) {
            return new CastDetail(castId, castName, castRole, castPhoto);
        }
    }

    public record StaffDetail(
            Long staffId,
            String staffName,
            String staffRole,
            String staffPhoto
    ) {
        public static StaffDetail of(Long staffId, String staffName, String staffRole, String staffPhoto) {
            return new StaffDetail(staffId, staffName, staffRole, staffPhoto);
        }
    }
}
