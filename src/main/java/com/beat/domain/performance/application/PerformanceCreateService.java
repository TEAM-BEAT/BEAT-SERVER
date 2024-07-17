package com.beat.domain.performance.application;

import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.performance.application.dto.create.CastResponse;
import com.beat.domain.performance.application.dto.create.PerformanceRequest;
import com.beat.domain.performance.application.dto.create.PerformanceResponse;
import com.beat.domain.performance.application.dto.create.ScheduleResponse;
import com.beat.domain.performance.application.dto.create.StaffResponse;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceCreateService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final CastRepository castRepository;
    private final StaffRepository staffRepository;

    @Transactional
    public PerformanceResponse createPerformance(Long userId, PerformanceRequest request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        Performance performance = Performance.create(
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
                request.ticketPrice(),
                request.totalScheduleCount(),
                user
        );
        performanceRepository.save(performance);

        List<Schedule> schedules = request.scheduleList().stream()
                .map(scheduleRequest -> Schedule.create(
                        scheduleRequest.performanceDate(),
                        scheduleRequest.totalTicketCount(),
                        0,
                        true,
                        scheduleRequest.scheduleNumber(),
                        performance
                ))
                .collect(Collectors.toList());
        scheduleRepository.saveAll(schedules);

        List<Cast> casts = request.castList().stream()
                .map(castRequest -> Cast.create(
                        castRequest.castName(),
                        castRequest.castRole(),
                        castRequest.castPhoto(),
                        performance
                ))
                .collect(Collectors.toList());
        castRepository.saveAll(casts);

        List<Staff> staffs = request.staffList().stream()
                .map(staffRequest -> Staff.create(
                        staffRequest.staffName(),
                        staffRequest.staffRole(),
                        staffRequest.staffPhoto(),
                        performance
                ))
                .collect(Collectors.toList());
        staffRepository.saveAll(staffs);

        return mapToPerformanceResponse(performance, schedules, casts, staffs);
    }

    private PerformanceResponse mapToPerformanceResponse(Performance performance, List<Schedule> schedules, List<Cast> casts, List<Staff> staffs) {
        List<ScheduleResponse> scheduleResponses = schedules.stream()
                .map(schedule -> ScheduleResponse.of(
                        schedule.getId(),
                        schedule.getPerformanceDate(),
                        schedule.getTotalTicketCount(),
                        calculateDueDate(schedule.getPerformanceDate().toLocalDate()),
                        schedule.getScheduleNumber()
                ))
                .collect(Collectors.toList());

        List<CastResponse> castResponses = casts.stream()
                .map(cast -> CastResponse.of(
                        cast.getId(),
                        cast.getCastName(),
                        cast.getCastRole(),
                        cast.getCastPhoto()
                ))
                .collect(Collectors.toList());

        List<StaffResponse> staffResponses = staffs.stream()
                .map(staff -> StaffResponse.of(
                        staff.getId(),
                        staff.getStaffName(),
                        staff.getStaffRole(),
                        staff.getStaffPhoto()
                ))
                .collect(Collectors.toList());

        return PerformanceResponse.of(
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

    private int calculateDueDate(LocalDate performanceDate) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), performanceDate);
    }
}