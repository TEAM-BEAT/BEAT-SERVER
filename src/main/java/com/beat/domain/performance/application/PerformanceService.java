package com.beat.domain.performance.application;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.application.dto.*;
import com.beat.domain.performance.application.dto.home.HomePerformanceDetail;
import com.beat.domain.performance.application.dto.home.HomePromotionDetail;
import com.beat.domain.performance.application.dto.home.HomeRequest;
import com.beat.domain.performance.application.dto.home.HomeResponse;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.schedule.application.ScheduleService;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final CastRepository castRepository;
    private final StaffRepository staffRepository;
    private final ScheduleService scheduleService;
    private final PromotionRepository promotionRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public BookingPerformanceDetailResponse getBookingPerformanceDetail(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        List<BookingPerformanceDetailSchedule> scheduleList = scheduleRepository.findByPerformanceId(performanceId).stream()
                .map(schedule -> {
                    scheduleService.updateBookingStatus(schedule);
                    return BookingPerformanceDetailSchedule.of(
                            schedule.getId(),
                            schedule.getPerformanceDate(),
                            schedule.getScheduleNumber().name(),
                            scheduleService.getAvailableTicketCount(schedule),
                            schedule.isBooking()
                    );
                }).collect(Collectors.toList());

        return BookingPerformanceDetailResponse.of(
                performance.getId(),
                performance.getPerformanceTitle(),
                performance.getPerformancePeriod(),
                scheduleList,
                performance.getTicketPrice(),
                performance.getGenre().name(),
                performance.getPosterImage(),
                performance.getPerformanceVenue(),
                performance.getPerformanceTeamName(),
                performance.getBankName() != null ? performance.getBankName().name() : null,
                performance.getAccountNumber(),
                performance.getAccountHolder()
        );
    }

    @Transactional(readOnly = true)
    public HomeResponse getHomePerformanceList(HomeRequest homeRequest) {
        List<Performance> performances;

        if (homeRequest.genre() != null) {
            performances = performanceRepository.findByGenre(homeRequest.genre());
        } else {
            performances = performanceRepository.findAll();
        }

        if (performances.isEmpty()) {
            List<HomePromotionDetail> promotions = getPromotions();
            return HomeResponse.of(promotions, new ArrayList<>());
        }

        List<HomePerformanceDetail> performanceDetails = performances.stream()
                .map(performance -> {
                    List<Schedule> schedules = scheduleRepository.findByPerformanceId(performance.getId());
                    int minDueDate = scheduleService.getMinDueDate(schedules);

                    return HomePerformanceDetail.of(
                            performance.getId(),
                            performance.getPerformanceTitle(),
                            performance.getPerformancePeriod(),
                            performance.getTicketPrice(),
                            minDueDate,
                            performance.getGenre().name(),
                            performance.getPosterImage(),
                            performance.getPerformanceVenue()
                    );
                })
                .collect(Collectors.toList());

        // 두 개의 스트림을 각각 처리하여 병합
        List<HomePerformanceDetail> positiveDueDates = performanceDetails.stream()
                .filter(detail -> detail.dueDate() >= 0)
                .sorted((p1, p2) -> Integer.compare(p1.dueDate(), p2.dueDate()))
                .collect(Collectors.toList());

        List<HomePerformanceDetail> negativeDueDates = performanceDetails.stream()
                .filter(detail -> detail.dueDate() < 0)
                .sorted((p1, p2) -> Integer.compare(p2.dueDate(), p1.dueDate()))
                .collect(Collectors.toList());

        // 병합된 리스트
        positiveDueDates.addAll(negativeDueDates);

        List<HomePromotionDetail> promotions = getPromotions();

        return HomeResponse.of(promotions, positiveDueDates);
    }

    private List<HomePromotionDetail> getPromotions() {
        List<Promotion> promotionList = promotionRepository.findAll();
        return promotionList.stream()
                .map(promotion -> HomePromotionDetail.of(
                        promotion.getId(),
                        promotion.getPromotionPhoto(),
                        promotion.getPerformance().getId()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MakerPerformanceResponse getMemberPerformances(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        List<Performance> performances = performanceRepository.findByUsersId(user.getId());

        List<MakerPerformanceDetail> performanceDetails = performances.stream()
                .map(performance -> MakerPerformanceDetail.of(
                        performance.getId(),
                        performance.getGenre().name(),
                        performance.getPerformanceTitle(),
                        performance.getPosterImage(),
                        performance.getPerformancePeriod()
                ))
                .collect(Collectors.toList());

        return MakerPerformanceResponse.of(user.getId(), performanceDetails);
    }

}
