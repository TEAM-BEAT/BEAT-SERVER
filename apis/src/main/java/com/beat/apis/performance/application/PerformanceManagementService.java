package com.beat.apis.performance.application;

import static java.util.Comparator.comparing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.performance.application.dto.create.CastResponse;
import com.beat.apis.performance.application.dto.create.PerformanceImageResponse;
import com.beat.apis.performance.application.dto.create.PerformanceRequest;
import com.beat.apis.performance.application.dto.create.PerformanceResponse;
import com.beat.apis.performance.application.dto.create.ScheduleResponse;
import com.beat.apis.performance.application.dto.create.StaffResponse;
import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.cast.repository.CastRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.domain.PerformanceImage;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.performance.repository.PerformanceImageRepository;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.repository.StaffRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	private final PromotionRepository promotionRepository;
	private final ScheduleJobPort scheduleJobPort;

	@Transactional
	public PerformanceResponse createPerformance(Long memberId, PerformanceRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

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
			request.roadAddressName(),
			request.placeDetailAddress(),
			request.latitude(),
			request.longitude(),
			request.performanceContact(),
			" ", // 이후 dto performancePeriod 제외 필요
			request.ticketPrice(),
			request.totalScheduleCount(),
			member.getUserId()
		);
		Performance savedPerformance = performanceRepository.save(performance);
		final Long savedPerformanceId = savedPerformance.getId();

		List<Schedule> schedules = request.scheduleList().stream()
			.map(scheduleRequest -> {
				if (scheduleRequest.performanceDate().isBefore(LocalDateTime.now())) {
					throw new BadRequestException(PerformanceErrorCode.PAST_SCHEDULE_NOT_ALLOWED);
				}
				return Schedule.create(
					scheduleRequest.performanceDate(),
					scheduleRequest.totalTicketCount(),
					scheduleRequest.scheduleNumber(),
					savedPerformanceId
				);
			})
			.collect(Collectors.toList());

		assignScheduleNumbers(schedules);
		schedules = scheduleRepository.saveAll(schedules);

		schedules.forEach(scheduleJobPort::registerOrRefresh);

		List<LocalDateTime> performanceDates = schedules.stream()
			.map(Schedule::getPerformanceDate)
			.toList();
		savedPerformance = savedPerformance.updatePerformancePeriod(performanceDates);
		savedPerformance = performanceRepository.save(savedPerformance);

		List<Cast> casts = castRepository.saveAll(request.castList().stream()
			.map(castRequest -> Cast.create(
				castRequest.castName(),
				castRequest.castRole(),
				castRequest.castPhoto(),
				savedPerformanceId
			))
			.toList());

		List<Staff> staffs = staffRepository.saveAll(request.staffList().stream()
			.map(staffRequest -> Staff.create(
				staffRequest.staffName(),
				staffRequest.staffRole(),
				staffRequest.staffPhoto(),
				savedPerformanceId
			))
			.toList());

		List<PerformanceImage> performanceImageList = performanceImageRepository.saveAll(
			request.performanceImageList().stream()
				.map(performanceImageRequest -> PerformanceImage.create(
					performanceImageRequest.performanceImage(),
					savedPerformanceId
				))
				.toList()
		);

		return mapToPerformanceResponse(savedPerformance, schedules, casts, staffs, performanceImageList);
	}

	private PerformanceResponse mapToPerformanceResponse(Performance performance, List<Schedule> schedules,
		List<Cast> casts, List<Staff> staffs, List<PerformanceImage> performanceImages) {
		List<ScheduleResponse> scheduleResponses = schedules.stream()
			.map(schedule -> ScheduleResponse.of(
				schedule.getId(),
				schedule.getPerformanceDate(),
				schedule.getTotalTicketCount(),
				calculateDueDate(schedule.getPerformanceDate().toLocalDate()),
				schedule.getScheduleNumber()
			))
			.toList();

		List<CastResponse> castResponses = casts.stream()
			.map(cast -> CastResponse.of(
				cast.getId(),
				cast.getCastName(),
				cast.getCastRole(),
				cast.getCastPhoto()
			))
			.toList();

		List<StaffResponse> staffResponses = staffs.stream()
			.map(staff -> StaffResponse.of(
				staff.getId(),
				staff.getStaffName(),
				staff.getStaffRole(),
				staff.getStaffPhoto()
			))
			.toList();

		List<PerformanceImageResponse> performanceImageResponses = performanceImages.stream()
			.map(image -> PerformanceImageResponse.of(
				image.getId(),
				image.getPerformanceImageUrl()
			))
			.toList();

		return PerformanceResponse.of(
			performance.getUserId(),
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
			performance.getRoadAddressName(),
			performance.getPlaceDetailAddress(),
			performance.getLatitude(),
			performance.getLongitude(),
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

	private void assignScheduleNumbers(List<Schedule> schedules) {
		List<ScheduleNumber> scheduleNumbers = List.of(ScheduleNumber.values());
		if (schedules.size() > scheduleNumbers.size()) {
			throw new BadRequestException(PerformanceErrorCode.MAX_SCHEDULE_LIMIT_EXCEEDED);
		}
		schedules.sort(comparing(Schedule::getPerformanceDate));
		for (int i = 0; i < schedules.size(); i++) {
			schedules.set(i, schedules.get(i).updateScheduleNumber(scheduleNumbers.get(i)));
		}
	}

	private int calculateDueDate(LocalDate performanceDate) {
		return (int)ChronoUnit.DAYS.between(LocalDate.now(), performanceDate);
	}

	@Transactional
	public void deletePerformance(Long memberId, Long performanceId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

		Long userId = member.getUserId();

		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

		if (!Objects.equals(performance.getUserId(), userId)) {
			throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
		}

		List<Long> scheduleIds = scheduleRepository.findIdsByPerformanceId(performanceId);

		List<BookingStatus> inactiveStatuses = List.of(BookingStatus.BOOKING_CANCELLED, BookingStatus.BOOKING_DELETED);
		if (!scheduleIds.isEmpty()) {
			boolean isBookerExist = bookingRepository.existsActiveBookingByScheduleIds(scheduleIds, inactiveStatuses);

			if (isBookerExist) {
				throw new ForbiddenException(PerformanceErrorCode.PERFORMANCE_DELETE_FAILED);
			}

			int deletedInactiveBookingCount = bookingRepository.deleteInactiveBookingsByScheduleIds(scheduleIds,
				inactiveStatuses);
			log.debug("Deleted {} inactive bookings for performanceId={}", deletedInactiveBookingCount, performanceId);
		}

		// 모든 스케줄에 대해 등록된 TaskScheduler 작업을 취소
		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		for (Schedule schedule : schedules) {
			scheduleJobPort.cancel(schedule);
		}

		scheduleRepository.deleteByPerformanceId(performanceId);
		castRepository.deleteByPerformanceId(performanceId);
		staffRepository.deleteByPerformanceId(performanceId);
		promotionRepository.deleteByPerformanceId(performanceId);
		performanceImageRepository.deleteByPerformanceId(performanceId);
		performanceRepository.deleteById(performance.getId());
	}
}
