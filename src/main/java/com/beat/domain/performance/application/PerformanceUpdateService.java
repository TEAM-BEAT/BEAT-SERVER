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
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.exception.StaffErrorCode;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("Starting updatePerformance for memberId: {}, performanceId: {}", memberId, request.performanceId());

        validateMember(memberId);

        Performance performance = findPerformance(request.performanceId());

        updatePerformanceDetails(performance, request);

        List<ScheduleDeleteResponse> deletedSchedules = deleteSchedules(request.scheduleDeleteRequests());
        List<ScheduleUpdateResponse> updatedSchedules = updateSchedules(request.scheduleUpdateRequests());
        List<ScheduleAddResponse> addedSchedules = addSchedules(request.scheduleAddRequests(), performance);

        List<CastDeleteResponse> deletedCasts = deleteCasts(request.castDeleteRequests());
        List<CastUpdateResponse> updatedCasts = updateCasts(request.castUpdateRequests());
        List<CastAddResponse> addedCasts = addCasts(request.castAddRequests(), performance);

        List<StaffDeleteResponse> deletedStaffs = deleteStaffs(request.staffDeleteRequests());
        List<StaffUpdateResponse> updatedStaffs = updateStaffs(request.staffUpdateRequests());
        List<StaffAddResponse> addedStaffs = addStaffs(request.staffAddRequests(), performance);

        PerformanceUpdateResponse response = completeUpdateResponse(performance, addedSchedules, updatedSchedules, deletedSchedules,
                addedCasts, updatedCasts, deletedCasts,
                addedStaffs, updatedStaffs, deletedStaffs);

        log.info("Successfully completed updatePerformance for performanceId: {}", request.performanceId());
        return response;
    }

    private void validateMember(Long memberId) {
        log.debug("Validating memberId: {}", memberId);
        memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.error("Member not found: memberId: {}", memberId);
                    return new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND);
                });
    }

    private Performance findPerformance(Long performanceId) {
        log.debug("Finding performance with performanceId: {}", performanceId);
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> {
                    log.error("Performance not found: performanceId: {}", performanceId);
                    return new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
                });
    }

    private void updatePerformanceDetails(Performance performance, PerformanceUpdateRequest request) {
        log.debug("Updating performance details for performanceId: {}", performance.getId());
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
        log.debug("Performance details updated for performanceId: {}", performance.getId());
    }

    private List<ScheduleAddResponse> addSchedules(List<ScheduleAddRequest> requests, Performance performance) {
        log.debug("Adding schedules for performanceId: {}", performance.getId());
        if (requests == null || requests.isEmpty()) {
            log.debug("No schedules to add for performanceId: {}", performance.getId());
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Schedule schedule = Schedule.create(
                            request.performanceDate(),
                            request.totalTicketCount(),
                            0,
                            true,
                            request.scheduleNumber(),
                            performance
                    );
                    Schedule savedSchedule = scheduleRepository.save(schedule);
                    log.debug("Added schedule with scheduleId: {} for performanceId: {}", savedSchedule.getId(), performance.getId());
                    return ScheduleAddResponse.of(
                            savedSchedule.getId(),
                            savedSchedule.getPerformanceDate(),
                            savedSchedule.getTotalTicketCount(),
                            calculateDueDate(savedSchedule.getPerformanceDate()),
                            savedSchedule.getScheduleNumber()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<ScheduleUpdateResponse> updateSchedules(List<ScheduleUpdateRequest> requests) {
        log.debug("Updating schedules");
        if (requests == null || requests.isEmpty()) {
            log.debug("No schedules to update");
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Schedule schedule = scheduleRepository.findById(request.scheduleId())
                            .orElseThrow(() -> {
                                log.error("Schedule not found: scheduleId: {}", request.scheduleId());
                                return new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
                            });
                    schedule.update(
                            request.performanceDate(),
                            request.totalTicketCount(),
                            request.scheduleNumber()
                    );
                    scheduleRepository.save(schedule);
                    log.debug("Updated schedule with scheduleId: {}", schedule.getId());
                    return ScheduleUpdateResponse.of(
                            schedule.getId(),
                            schedule.getPerformanceDate(),
                            schedule.getTotalTicketCount(),
                            calculateDueDate(schedule.getPerformanceDate()),
                            schedule.getScheduleNumber()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<ScheduleDeleteResponse> deleteSchedules(List<ScheduleDeleteRequest> requests) {
        log.debug("Deleting schedules");
        if (requests == null || requests.isEmpty()) {
            log.debug("No schedules to delete");
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Schedule schedule = scheduleRepository.findById(request.scheduleId())
                            .orElseThrow(() -> {
                                log.error("Schedule not found: scheduleId: {}", request.scheduleId());
                                return new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
                            });
                    scheduleRepository.delete(schedule);
                    log.debug("Deleted schedule with scheduleId: {}", schedule.getId());
                    return ScheduleDeleteResponse.from(schedule.getId());
                })
                .collect(Collectors.toList());
    }

    private List<CastAddResponse> addCasts(List<CastAddRequest> requests, Performance performance) {
        log.debug("Adding casts for performanceId: {}", performance.getId());
        if (requests == null || requests.isEmpty()) {
            log.debug("No casts to add for performanceId: {}", performance.getId());
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Cast cast = Cast.create(
                            request.castName(),
                            request.castRole(),
                            request.castPhoto(),
                            performance
                    );
                    Cast savedCast = castRepository.save(cast);
                    log.debug("Added cast with castId: {} for performanceId: {}", savedCast.getId(), performance.getId());
                    return CastAddResponse.of(
                            savedCast.getId(),
                            savedCast.getCastName(),
                            savedCast.getCastRole(),
                            savedCast.getCastPhoto()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<CastUpdateResponse> updateCasts(List<CastUpdateRequest> requests) {
        log.debug("Updating casts");
        if (requests == null || requests.isEmpty()) {
            log.debug("No casts to update");
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Cast cast = castRepository.findById(request.castId())
                            .orElseThrow(() -> {
                                log.error("Cast not found: castId: {}", request.castId());
                                return new NotFoundException(CastErrorCode.CAST_NOT_FOUND);
                            });
                    cast.update(
                            request.castName(),
                            request.castRole(),
                            request.castPhoto()
                    );
                    castRepository.save(cast);
                    log.debug("Updated cast with castId: {}", cast.getId());
                    return CastUpdateResponse.of(
                            cast.getId(),
                            cast.getCastName(),
                            cast.getCastRole(),
                            cast.getCastPhoto()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<CastDeleteResponse> deleteCasts(List<CastDeleteRequest> requests) {
        log.debug("Deleting casts");
        if (requests == null || requests.isEmpty()) {
            log.debug("No casts to delete");
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Cast cast = castRepository.findById(request.castId())
                            .orElseThrow(() -> {
                                log.error("Cast not found: castId: {}", request.castId());
                                return new NotFoundException(CastErrorCode.CAST_NOT_FOUND);
                            });
                    castRepository.delete(cast);
                    log.debug("Deleted cast with castId: {}", cast.getId());
                    return CastDeleteResponse.from(cast.getId());
                })
                .collect(Collectors.toList());
    }

    private List<StaffAddResponse> addStaffs(List<StaffAddRequest> requests, Performance performance) {
        log.debug("Adding staffs for performanceId: {}", performance.getId());
        if (requests == null || requests.isEmpty()) {
            log.debug("No staffs to add for performanceId: {}", performance.getId());
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Staff staff = Staff.create(
                            request.staffName(),
                            request.staffRole(),
                            request.staffPhoto(),
                            performance
                    );
                    Staff savedStaff = staffRepository.save(staff);
                    log.debug("Added staff with staffId: {} for performanceId: {}", savedStaff.getId(), performance.getId());
                    return StaffAddResponse.of(
                            savedStaff.getId(),
                            savedStaff.getStaffName(),
                            savedStaff.getStaffRole(),
                            savedStaff.getStaffPhoto()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<StaffUpdateResponse> updateStaffs(List<StaffUpdateRequest> requests) {
        log.debug("Updating staffs");
        if (requests == null || requests.isEmpty()) {
            log.debug("No staffs to update");
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Staff staff = staffRepository.findById(request.staffId())
                            .orElseThrow(() -> {
                                log.error("Staff not found: staffId: {}", request.staffId());
                                return new NotFoundException(StaffErrorCode.STAFF_NOT_FOUND);
                            });
                    staff.update(
                            request.staffName(),
                            request.staffRole(),
                            request.staffPhoto()
                    );
                    staffRepository.save(staff);
                    log.debug("Updated staff with staffId: {}", staff.getId());
                    return StaffUpdateResponse.of(
                            staff.getId(),
                            staff.getStaffName(),
                            staff.getStaffRole(),
                            staff.getStaffPhoto()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<StaffDeleteResponse> deleteStaffs(List<StaffDeleteRequest> requests) {
        log.debug("Deleting staffs");
        if (requests == null || requests.isEmpty()) {
            log.debug("No staffs to delete");
            return List.of();
        }

        return requests.stream()
                .map(request -> {
                    Staff staff = staffRepository.findById(request.staffId())
                            .orElseThrow(() -> {
                                log.error("Staff not found: staffId: {}", request.staffId());
                                return new NotFoundException(StaffErrorCode.STAFF_NOT_FOUND);
                            });
                    staffRepository.delete(staff);
                    log.debug("Deleted staff with staffId: {}", staff.getId());
                    return StaffDeleteResponse.from(staff.getId());
                })
                .collect(Collectors.toList());
    }

    private int calculateDueDate(LocalDateTime performanceDate) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), performanceDate.toLocalDate());
    }

    private PerformanceUpdateResponse completeUpdateResponse(
            Performance performance,
            List<ScheduleAddResponse> addedSchedules,
            List<ScheduleUpdateResponse> updatedSchedules,
            List<ScheduleDeleteResponse> deletedSchedules,
            List<CastAddResponse> addedCasts,
            List<CastUpdateResponse> updatedCasts,
            List<CastDeleteResponse> deletedCasts,
            List<StaffAddResponse> addedStaffs,
            List<StaffUpdateResponse> updatedStaffs,
            List<StaffDeleteResponse> deletedStaffs
    ) {
        log.debug("Creating PerformanceUpdateResponse for performanceId: {}", performance.getId());
        PerformanceUpdateResponse response = PerformanceUpdateResponse.of(
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
        log.info("PerformanceUpdateResponse created successfully for performanceId: {}", performance.getId());
        return response;
    }
}