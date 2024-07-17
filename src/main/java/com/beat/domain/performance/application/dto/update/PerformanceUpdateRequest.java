package com.beat.domain.performance.application.dto.update;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import java.util.List;

public record PerformanceUpdateRequest(
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
        int totalScheduleCount,
        List<ScheduleUpdateRequest> scheduleList,
        List<CastUpdateRequest> castList,
        List<StaffUpdateRequest> staffList
) {}
