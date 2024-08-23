package com.beat.domain.performance.application.dto.modify;

import com.beat.domain.performance.application.dto.modify.cast.CastModifyRequest;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyRequest;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyRequest;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import java.util.List;

public record PerformanceModifyRequest(
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
        int ticketPrice,
        List<ScheduleModifyRequest> scheduleModifyRequests,
        List<CastModifyRequest> castModifyRequests,
        List<StaffModifyRequest> staffModifyRequests
) {}