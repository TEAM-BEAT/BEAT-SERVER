package com.beat.domain.performance.application;

import com.beat.domain.performance.application.dto.PerformanceDetailCast;
import com.beat.domain.performance.application.dto.PerformanceDetailResponse;
import com.beat.domain.performance.application.dto.PerformanceDetailSchedule;
import com.beat.domain.performance.application.dto.PerformanceDetailStaff;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final CastRepository castRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        List<PerformanceDetailSchedule> scheduleList = scheduleRepository.findByPerformanceId(performanceId).stream()
                .map(schedule -> PerformanceDetailSchedule.of(
                        schedule.getId(),
                        schedule.getPerformanceDate(),
                        schedule.getScheduleNumber().name()
                )).collect(Collectors.toList());

        List<PerformanceDetailCast> castList = castRepository.findByPerformanceId(performanceId).stream()
                .map(cast -> PerformanceDetailCast.of(
                        cast.getId(),
                        cast.getCastName(),
                        cast.getCastRole(),
                        cast.getCastPhoto()
                )).collect(Collectors.toList());

        List<PerformanceDetailStaff> staffList = staffRepository.findByPerformanceId(performanceId).stream()
                .map(staff -> PerformanceDetailStaff.of(
                        staff.getId(),
                        staff.getStaffName(),
                        staff.getStaffRole(),
                        staff.getStaffPhoto()
                )).collect(Collectors.toList());

        return PerformanceDetailResponse.of(
                performance.getId(),
                performance.getPerformanceTitle(),
                performance.getPerformancePeriod(),
                scheduleList,
                performance.getTicketPrice(),
                performance.getGenre().name(),
                performance.getPosterImage(),
                performance.getRunningTime(),
                performance.getPerformanceVenue(),
                performance.getPerformanceDescription(),
                performance.getPerformanceAttentionNote(),
                performance.getPerformanceContact(),
                performance.getPerformanceTeamName(),
                castList,
                staffList
        );
    }
}
