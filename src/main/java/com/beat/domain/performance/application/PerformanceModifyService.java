package com.beat.domain.performance.application;

import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.cast.exception.CastErrorCode;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.application.dto.modify.*;
import com.beat.domain.performance.application.dto.modify.cast.CastModifyRequest;
import com.beat.domain.performance.application.dto.modify.cast.CastModifyResponse;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyRequest;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyResponse;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyRequest;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyResponse;
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
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceModifyService {
    
    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final CastRepository castRepository;
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PerformanceModifyResponse modifyPerformance(Long memberId, PerformanceModifyRequest request) {
        log.info("Starting updatePerformance for memberId: {}, performanceId: {}", memberId, request.performanceId());

        Member member = validateMember(memberId);
        Long userId = member.getUser().getId();

        Performance performance = findPerformance(request.performanceId());

        validateOwnership(userId, performance);

        List<Long> scheduleIds = scheduleRepository.findIdsByPerformanceId(request.performanceId());
        boolean isBookerExist = bookingRepository.existsByScheduleIdIn(scheduleIds);

        if (isBookerExist && request.ticketPrice() != performance.getTicketPrice()) {
            log.error("Ticket price update failed due to existing bookings for performanceId: {}", performance.getId());
            throw new BadRequestException(PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED);
        }

        updatePerformanceDetails(performance, request, isBookerExist);

        List<ScheduleModifyResponse> modifiedSchedules = processSchedules(request.scheduleModifyRequests(), performance);
        List<CastModifyResponse> modifiedCasts = processCasts(request.castModifyRequests(), performance);
        List<StaffModifyResponse> modifiedStaffs = processStaffs(request.staffModifyRequests(), performance);

        PerformanceModifyResponse response = completeModifyResponse(performance, modifiedSchedules, modifiedCasts, modifiedStaffs);

        log.info("Successfully completed updatePerformance for performanceId: {}", request.performanceId());
        return response;
    }

    private Member validateMember(Long memberId) {
        log.debug("Validating memberId: {}", memberId);
        return memberRepository.findById(memberId)
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

    private void validateOwnership(Long userId, Performance performance) {
        if (!performance.getUsers().getId().equals(userId)) {
            log.error("User ID {} does not own performance ID {}", userId, performance.getId());
            throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
        }
    }

    private void updatePerformanceDetails(Performance performance, PerformanceModifyRequest request, boolean isBookerExist) {
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

        if (!isBookerExist) {
            log.debug("Updating ticket price to {}", request.ticketPrice());
            performance.updateTicketPrice(request.ticketPrice());
        }

        performanceRepository.save(performance);
        log.debug("Performance details updated for performanceId: {}", performance.getId());
    }

    private List<ScheduleModifyResponse> processSchedules(List<ScheduleModifyRequest> scheduleRequests, Performance performance) {
        // 현재 존재하는 스케줄 ID를 가져옵니다.
        List<Long> existingScheduleIds = scheduleRepository.findIdsByPerformanceId(performance.getId());

        // 스케줄 요청에 따라 추가 또는 업데이트된 스케줄 객체를 생성합니다.
        List<Schedule> schedules = scheduleRequests.stream()
                .map(request -> {
                    if (request.scheduleId() == null) {
                        return addSchedule(request, performance);
                    } else {
                        existingScheduleIds.remove(request.scheduleId());
                        return updateSchedule(request);
                    }
                })
                .collect(Collectors.toList());

        // 요청에 포함되지 않은 기존 스케줄은 삭제합니다.
        deleteRemainingSchedules(existingScheduleIds);

        // 스케줄 번호를 할당합니다.
        assignScheduleNumbers(schedules);

        // Schedule 리스트를 ScheduleModifyResponse 리스트로 변환하여 반환합니다.
        return schedules.stream()
                .map(schedule -> ScheduleModifyResponse.of(
                        schedule.getId(),
                        schedule.getPerformanceDate(),
                        schedule.getTotalTicketCount(),
                        calculateDueDate(schedule.getPerformanceDate()),
                        schedule.getScheduleNumber()
                ))
                .collect(Collectors.toList());
    }

    private void assignScheduleNumbers(List<Schedule> schedules) {
        // 스케줄을 날짜 순서대로 정렬합니다.
        schedules.sort(Comparator.comparing(Schedule::getPerformanceDate));

        // 각 스케줄에 번호를 부여합니다.
        for (int i = 0; i < schedules.size(); i++) {
            ScheduleNumber scheduleNumber = ScheduleNumber.values()[i];
            schedules.get(i).updateScheduleNumber(scheduleNumber);
        }
    }

    private Schedule addSchedule(ScheduleModifyRequest request, Performance performance) {
        log.debug("Adding schedules for performanceId: {}", performance.getId());

        long existingSchedulesCount = scheduleRepository.countByPerformanceId(performance.getId());

        // 스케줄 최대 개수 초과 여부 확인
        if ((existingSchedulesCount + 1) > 3) {
            throw new BadRequestException(PerformanceErrorCode.MAX_SCHEDULE_LIMIT_EXCEEDED);
        }

        Schedule schedule = Schedule.create(
                request.performanceDate(),
                request.totalTicketCount(),
                0,
                true,
                ScheduleNumber.FIRST, // 임시로 1회차
                performance
        );

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.debug("Added schedule with scheduleId: {} for performanceId: {}", savedSchedule.getId(), performance.getId());
        return savedSchedule;
    }

    private Schedule updateSchedule(ScheduleModifyRequest request) {
        log.debug("Updating schedules for scheduleId: {}", request.scheduleId());

        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> {
                    log.error("Schedule not found: scheduleId: {}", request.scheduleId());
                    return new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
                });
        schedule.update(
                request.performanceDate(),
                request.totalTicketCount(),
                schedule.getScheduleNumber()  // 기존 scheduleNumber 유지
        );
        return scheduleRepository.save(schedule);
    }

    private void deleteRemainingSchedules(List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            log.debug("No schedules to delete");
            return;
        }

        scheduleIds.forEach(scheduleId -> {
            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> {
                        log.error("Schedule not found: scheduleId: {}", scheduleId);
                        return new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
                    });
            scheduleRepository.delete(schedule);
            log.debug("Deleted schedule with scheduleId: {}", scheduleId);
        });
    }

    private List<CastModifyResponse> processCasts(List<CastModifyRequest> castRequests, Performance performance) {
        log.debug("Processing casts for performanceId: {}", performance.getId());

        List<Long> existingCastIds = castRepository.findIdsByPerformanceId(performance.getId());

        List<CastModifyResponse> responses = castRequests.stream()
                .map(request -> {
                    if (request.castId() == null) {
                        return addCast(request, performance);
                    } else {
                        existingCastIds.remove(request.castId()); // 요청에 포함된 ID는 삭제 후보에서 제거
                        return updateCast(request);
                    }
                })
                .collect(Collectors.toList());

        deleteRemainingCasts(existingCastIds);

        return responses;
    }

    private CastModifyResponse addCast(CastModifyRequest request, Performance performance) {
        log.debug("Adding casts for performanceId: {}", performance.getId());

        Cast cast = Cast.create(
                request.castName(),
                request.castRole(),
                request.castPhoto(),
                performance
        );
        Cast savedCast = castRepository.save(cast);
        log.debug("Added cast with castId: {} for performanceId: {}", savedCast.getId(), performance.getId());
        return CastModifyResponse.of(
                savedCast.getId(),
                savedCast.getCastName(),
                savedCast.getCastRole(),
                savedCast.getCastPhoto()
        );
    }

    private CastModifyResponse updateCast(CastModifyRequest request) {
        log.debug("Updating casts for castId: {}", request.castId());

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
        return CastModifyResponse.of(
                cast.getId(),
                cast.getCastName(),
                cast.getCastRole(),
                cast.getCastPhoto()
        );
    }

    private void deleteRemainingCasts(List<Long> castIds) {
        if (castIds == null || castIds.isEmpty()) {
            log.debug("No casts to delete");
            return;
        }

        castIds.forEach(castId -> {
            Cast cast = castRepository.findById(castId)
                    .orElseThrow(() -> {
                        log.error("Cast not found: castId: {}", castId);
                        return new NotFoundException(CastErrorCode.CAST_NOT_FOUND);
                    });
            castRepository.delete(cast);
            log.debug("Deleted cast with castId: {}", castId);
        });
    }

    private List<StaffModifyResponse> processStaffs(List<StaffModifyRequest> staffRequests, Performance performance) {
        log.debug("Processing staffs for performanceId: {}", performance.getId());

        List<Long> existingStaffIds = staffRepository.findIdsByPerformanceId(performance.getId());

        List<StaffModifyResponse> responses = staffRequests.stream()
                .map(request -> {
                    if (request.staffId() == null) {
                        return addStaff(request, performance);
                    } else {
                        existingStaffIds.remove(request.staffId()); // 요청에 포함된 ID는 삭제 후보에서 제거
                        return updateStaff(request);
                    }
                })
                .collect(Collectors.toList());

        deleteRemainingStaffs(existingStaffIds);

        return responses;
    }

    private StaffModifyResponse addStaff(StaffModifyRequest request, Performance performance) {
        log.debug("Adding staffs for performanceId: {}", performance.getId());

        Staff staff = Staff.create(
                request.staffName(),
                request.staffRole(),
                request.staffPhoto(),
                performance
        );
        Staff savedStaff = staffRepository.save(staff);
        log.debug("Added staff with staffId: {} for performanceId: {}", savedStaff.getId(), performance.getId());
        return StaffModifyResponse.of(
                savedStaff.getId(),
                savedStaff.getStaffName(),
                savedStaff.getStaffRole(),
                savedStaff.getStaffPhoto()
        );
    }

    private StaffModifyResponse updateStaff(StaffModifyRequest request) {
        log.debug("Updating staffs for staffId: {}", request.staffId());

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
        return StaffModifyResponse.of(
                staff.getId(),
                staff.getStaffName(),
                staff.getStaffRole(),
                staff.getStaffPhoto()
        );
    }

    private void deleteRemainingStaffs(List<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            log.debug("No staffs to delete");
            return;
        }

        staffIds.forEach(staffId -> {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> {
                        log.error("Staff not found: staffId: {}", staffId);
                        return new NotFoundException(StaffErrorCode.STAFF_NOT_FOUND);
                    });
            staffRepository.delete(staff);
            log.debug("Deleted staff with staffId: {}", staffId);
        });
    }

    private int calculateDueDate(LocalDateTime performanceDate) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), performanceDate.toLocalDate());
    }

    private PerformanceModifyResponse completeModifyResponse(
            Performance performance,
            List<ScheduleModifyResponse> scheduleModifyResponses,
            List<CastModifyResponse> castModifyResponses,
            List<StaffModifyResponse> staffModifyResponses
    ) {
        log.debug("Creating PerformanceModifyResponse for performanceId: {}", performance.getId());
        PerformanceModifyResponse response = PerformanceModifyResponse.of(
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
                scheduleModifyResponses,
                castModifyResponses,
                staffModifyResponses
        );
        log.info("PerformanceModifyResponse created successfully for performanceId: {}", performance.getId());
        return response;
    }
}