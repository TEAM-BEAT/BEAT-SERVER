package com.beat.domain.performance.application;

import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.application.dto.create.*;
import com.beat.domain.performance.dao.PerformanceImageRepository;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.domain.PerformanceImage;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
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
public class PerformanceManagementService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final CastRepository castRepository;
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final PerformanceImageRepository performanceImageRepository;

    @Transactional
    public PerformanceResponse createPerformance(Long memberId, PerformanceRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = member.getUser();

        if (request.performanceDescription().length() > 500) {
            throw new BadRequestException(PerformanceErrorCode.INVALID_PERFORMANCE_DESCRIPTION_LENGTH);
        }

        if (request.performanceAttentionNote().length() > 500) {
            throw new BadRequestException(PerformanceErrorCode.INVALID_ATTENTION_NOTE_LENGTH);
        }

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

        List<PerformanceImage> performanceImageList = request.performanceImageList().stream()
                .map(performanceImageRequest -> PerformanceImage.create(
                        performanceImageRequest.performanceImage(),
                        performance
                ))
                .collect(Collectors.toList());
        performanceImageRepository.saveAll(performanceImageList);

        return mapToPerformanceResponse(performance, schedules, casts, staffs, performanceImageList);
    }

    private PerformanceResponse mapToPerformanceResponse(Performance performance, List<Schedule> schedules, List<Cast> casts, List<Staff> staffs, List<PerformanceImage> performanceImages) {
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

        List<PerformanceImageResponse> performanceImageResponses = performanceImages.stream()
                .map(image -> PerformanceImageResponse.of(
                        image.getId(),
                        image.getPerformanceImage()
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
                staffResponses,
                performanceImageResponses
        );
    }

    private int calculateDueDate(LocalDate performanceDate) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), performanceDate);
    }

    @Transactional
    public void deletePerformance(Long memberId, Long performanceId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Long userId = member.getUser().getId();

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        if (!performance.getUsers().getId().equals(userId)) {
            throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
        }

        List<Long> scheduleIds = scheduleRepository.findIdsByPerformanceId(performanceId);

        boolean hasBookings = bookingRepository.existsByScheduleIdIn(scheduleIds);

        if (hasBookings) {
            throw new ForbiddenException(PerformanceErrorCode.PERFORMANCE_DELETE_FAILED);
        }

        performanceRepository.delete(performance);
    }
}