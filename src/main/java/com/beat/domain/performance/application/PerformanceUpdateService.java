package com.beat.domain.performance.application;

import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.cast.exception.CastErrorCode;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.application.dto.update.*;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.exception.StaffErrorCode;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceUpdateService {
    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final CastRepository castRepository;
    private final StaffRepository staffRepository;

    @Transactional
    public PerformanceUpdateResponse updatePerformance(Long memberId, PerformanceUpdateRequest request) {
        memberRepository.findById(memberId).orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Performance performance = performanceRepository.findById(request.performanceId())
                .orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        performance.update(
                request.performanceTitle(),
                request.genre(),
                request.runningTime(),
                request.performanceDescription(),
                request.performanceAttentionNote(),
                request.bankName(),
                request.accountNumber(),
                request.accountHolder(),
                request.posterImage(),
                request.performanceTeamName(),
                request.performanceVenue(),
                request.performanceContact(),
                request.performancePeriod(),
                request.totalScheduleCount()
        );
        performanceRepository.save(performance);

        List<Schedule> schedules = request.scheduleList().stream()
                .map(scheduleRequest -> {
                    Schedule schedule = scheduleRepository.findById(scheduleRequest.scheduleId())
                            .orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));
                    schedule.update(
                            scheduleRequest.performanceDate(),
                            scheduleRequest.totalTicketCount(),
                            ScheduleNumber.valueOf(scheduleRequest.scheduleNumber())
                    );
                    return schedule;
                })
                .collect(Collectors.toList());
        scheduleRepository.saveAll(schedules);

        List<Cast> casts = request.castList().stream()
                .map(castRequest -> {
                    Cast cast = castRepository.findById(castRequest.castId())
                            .orElseThrow(() -> new NotFoundException(CastErrorCode.CAST_NOT_FOUND));
                    cast.update(
                            castRequest.castName(),
                            castRequest.castRole(),
                            castRequest.castPhoto()
                    );
                    return cast;
                })
                .collect(Collectors.toList());
        castRepository.saveAll(casts);

        List<Staff> staffs = request.staffList().stream()
                .map(staffRequest -> {
                    Staff staff = staffRepository.findById(staffRequest.staffId())
                            .orElseThrow(() -> new NotFoundException(StaffErrorCode.STAFF_NOT_FOUND));
                    staff.update(
                            staffRequest.staffName(),
                            staffRequest.staffRole(),
                            staffRequest.staffPhoto()
                    );
                    return staff;
                })
                .collect(Collectors.toList());
        staffRepository.saveAll(staffs);

        return mapToPerformanceResponse(performance, schedules, casts, staffs);
    }

    private PerformanceUpdateResponse mapToPerformanceResponse(Performance performance, List<Schedule> schedules, List<Cast> casts, List<Staff> staffs) {
        List<ScheduleUpdateResponse> scheduleResponses = schedules.stream()
                .map(schedule -> ScheduleUpdateResponse.of(
                        schedule.getId(),
                        schedule.getPerformanceDate(),
                        schedule.getTotalTicketCount(),
                        calculateDueDate(schedule.getPerformanceDate()),
                        schedule.getScheduleNumber()
                ))
                .collect(Collectors.toList());

        List<CastUpdateResponse> castResponses = casts.stream()
                .map(cast -> CastUpdateResponse.of(
                        cast.getId(),
                        cast.getCastName(),
                        cast.getCastRole(),
                        cast.getCastPhoto()
                ))
                .collect(Collectors.toList());

        List<StaffUpdateResponse> staffResponses = staffs.stream()
                .map(staff -> StaffUpdateResponse.of(
                        staff.getId(),
                        staff.getStaffName(),
                        staff.getStaffRole(),
                        staff.getStaffPhoto()
                ))
                .collect(Collectors.toList());

        return PerformanceUpdateResponse.of(
                performance.getUsers().getId(),
                performance.getId(),
                performance.getPerformanceTitle(),
                performance.getGenre(),
                performance.getRunningTime(),
                performance.getPerformanceDescription(),
                performance.getPerformanceAttentionNote(),
                performance.getBankName(),
                performance.getAccountNumber(),
                performance.getAccountHolder(),
                performance.getPosterImage(),
                performance.getPerformanceTeamName(),
                performance.getPerformanceVenue(),
                performance.getPerformanceContact(),
                performance.getPerformancePeriod(),
                performance.getTicketPrice(),
                performance.getTotalScheduleCount(),
                scheduleResponses,
                castResponses,
                staffResponses
        );
    }

    private int calculateDueDate(LocalDateTime performanceDate) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), performanceDate.toLocalDate());
    }
}