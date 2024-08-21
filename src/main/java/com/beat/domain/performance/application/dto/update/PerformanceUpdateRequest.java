package com.beat.domain.performance.application.dto.update;

import com.beat.domain.performance.application.dto.update.cast.CastAddRequest;
import com.beat.domain.performance.application.dto.update.cast.CastDeleteRequest;
import com.beat.domain.performance.application.dto.update.cast.CastUpdateRequest;
import com.beat.domain.performance.application.dto.update.schedule.ScheduleAddRequest;
import com.beat.domain.performance.application.dto.update.schedule.ScheduleDeleteRequest;
import com.beat.domain.performance.application.dto.update.schedule.ScheduleUpdateRequest;
import com.beat.domain.performance.application.dto.update.staff.StaffAddRequest;
import com.beat.domain.performance.application.dto.update.staff.StaffDeleteRequest;
import com.beat.domain.performance.application.dto.update.staff.StaffUpdateRequest;
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
        List<ScheduleAddRequest> scheduleAddRequests,
        List<ScheduleDeleteRequest> scheduleDeleteRequests,
        List<ScheduleUpdateRequest> scheduleUpdateRequests,
        List<CastAddRequest> castAddRequests,
        List<CastDeleteRequest> castDeleteRequests,
        List<CastUpdateRequest> castUpdateRequests,
        List<StaffAddRequest> staffAddRequests,
        List<StaffDeleteRequest> staffDeleteRequests,
        List<StaffUpdateRequest> staffUpdateRequests
) {}
