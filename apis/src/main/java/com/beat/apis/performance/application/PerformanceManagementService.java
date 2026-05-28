package com.beat.apis.performance.application;

import static java.util.Comparator.comparing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.common.application.converter.BankNameEnumConverter;
import com.beat.apis.common.application.converter.GenreEnumConverter;
import com.beat.apis.common.application.converter.ScheduleNumberEnumConverter;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.performance.application.dto.create.CastResponse;
import com.beat.apis.performance.application.dto.create.PerformanceImageResponse;
import com.beat.apis.performance.application.dto.create.PerformanceRequest;
import com.beat.apis.performance.application.dto.create.PerformanceResponse;
import com.beat.apis.performance.application.dto.create.ScheduleResponse;
import com.beat.apis.performance.application.dto.create.StaffResponse;
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode;
import com.beat.contracts.cdn.ImageCachePort;
import com.beat.contracts.schedule.ScheduleBookingCloseJobPort;
import com.beat.contracts.schedule.ScheduleBookingCloseJobTarget;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.cast.repository.CastRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.performanceimage.domain.PerformanceImage;
import com.beat.domain.performanceimage.repository.PerformanceImageRepository;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.service.ScheduleDomainService;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.repository.StaffRepository;
import com.beat.global.support.exception.BadRequestException;
import com.beat.global.support.exception.ForbiddenException;
import com.beat.global.support.exception.NotFoundException;
import com.beat.global.support.utils.ImageKeyExtractor;

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
	private final ScheduleBookingCloseJobPort scheduleBookingCloseJobPort;
	private final ImageCachePort imageCachePort;
	private final ScheduleDomainService scheduleDomainService = new ScheduleDomainService();

	@Transactional
	public PerformanceResponse createPerformance(Long memberId, PerformanceRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		Performance performance = Performance.create(
			request.performanceTitle(),
			GenreEnumConverter.toDomain(request.genre()),
			request.runningTime(),
			request.performanceDescription(),
			request.performanceAttentionNote(),
			BankNameEnumConverter.toDomain(request.bankName()),
			request.accountNumber(),
			request.accountHolder(),
			ImageKeyExtractor.extract(request.posterImage()),
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
					throw new BadRequestException(PerformanceApplicationErrorCode.PAST_SCHEDULE_NOT_ALLOWED);
				}
				return Schedule.create(
					scheduleRequest.performanceDate(),
					scheduleRequest.totalTicketCount(),
					ScheduleNumberEnumConverter.toDomain(scheduleRequest.scheduleNumber()),
					savedPerformanceId
				);
			})
			.collect(Collectors.toList());

		assignScheduleNumbers(schedules);
		schedules = scheduleRepository.saveAll(schedules);

		schedules.stream()
			.map(this::toScheduleBookingCloseJobTarget)
			.forEach(scheduleBookingCloseJobPort::registerOrRefresh);

		List<LocalDateTime> performanceDates = schedules.stream()
			.map(Schedule::getPerformanceDate)
			.toList();
		if (performanceDates.isEmpty()) {
			throw new BadRequestException(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND);
		}
		savedPerformance = savedPerformance.updatePerformancePeriod(performanceDates);
		savedPerformance = performanceRepository.save(savedPerformance);

		List<Cast> casts = castRepository.saveAll(request.castList().stream()
			.map(castRequest -> Cast.create(
				castRequest.castName(),
				castRequest.castRole(),
				ImageKeyExtractor.extract(castRequest.castPhoto()),
				savedPerformanceId
			))
			.toList());

		List<Staff> staffs = staffRepository.saveAll(request.staffList().stream()
			.map(staffRequest -> Staff.create(
				staffRequest.staffName(),
				staffRequest.staffRole(),
				ImageKeyExtractor.extract(staffRequest.staffPhoto()),
				savedPerformanceId
			))
			.toList());

		List<PerformanceImage> performanceImageList = performanceImageRepository.saveAll(
			request.performanceImageList().stream()
				.map(performanceImageRequest -> PerformanceImage.create(
					ImageKeyExtractor.extract(performanceImageRequest.performanceImage()),
					savedPerformanceId
				))
				.toList()
		);

		imageCachePort.preWarm(savedPerformance.getPosterImage());

		return mapToPerformanceResponse(savedPerformance, schedules, casts, staffs, performanceImageList);
	}

	private ScheduleBookingCloseJobTarget toScheduleBookingCloseJobTarget(Schedule schedule) {
		return new ScheduleBookingCloseJobTarget(schedule.getId());
	}

	private PerformanceResponse mapToPerformanceResponse(Performance performance, List<Schedule> schedules,
		List<Cast> casts, List<Staff> staffs, List<PerformanceImage> performanceImages) {
		LocalDate today = LocalDate.now();
		List<ScheduleResponse> scheduleResponses = schedules.stream()
			.map(schedule -> ScheduleResponse.of(
				schedule.getId(),
				schedule.getPerformanceDate(),
				schedule.getTotalTicketCount(),
				scheduleDomainService.calculateDueDate(today, schedule),
				ScheduleNumberEnumConverter.toApi(schedule.getScheduleNumber())
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
			GenreEnumConverter.toPerformanceApi(performance.getGenre()),
			performance.getRunningTime(),
			performance.getPerformanceDescription(),
			performance.getPerformanceAttentionNote(),
			BankNameEnumConverter.toApi(performance.getBankName()),
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
			throw new BadRequestException(PerformanceApplicationErrorCode.MAX_SCHEDULE_LIMIT_EXCEEDED);
		}
		schedules.sort(comparing(Schedule::getPerformanceDate));
		for (int i = 0; i < schedules.size(); i++) {
			schedules.set(i, schedules.get(i).updateScheduleNumber(scheduleNumbers.get(i)));
		}
	}

	@Transactional
	public void deletePerformance(Long memberId, Long performanceId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		Long userId = member.getUserId();

		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND));

		if (!Objects.equals(performance.getUserId(), userId)) {
			throw new ForbiddenException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER);
		}

		List<Long> scheduleIds = scheduleRepository.findIdsByPerformanceId(performanceId);

		List<BookingStatus> inactiveStatuses = List.of(BookingStatus.BOOKING_CANCELLED, BookingStatus.BOOKING_DELETED);
		if (!scheduleIds.isEmpty()) {
			boolean isBookerExist = bookingRepository.existsActiveBookingByScheduleIds(scheduleIds, inactiveStatuses);

			if (isBookerExist) {
				throw new ForbiddenException(PerformanceApplicationErrorCode.PERFORMANCE_DELETE_FAILED);
			}

			int deletedInactiveBookingCount = bookingRepository.deleteInactiveBookingsByScheduleIds(scheduleIds,
				inactiveStatuses);
			log.debug("Deleted {} inactive bookings for performanceId={}", deletedInactiveBookingCount, performanceId);
		}

		// 모든 스케줄에 대해 등록된 TaskScheduler 작업을 취소
		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		for (Schedule schedule : schedules) {
			scheduleBookingCloseJobPort.cancel(toScheduleBookingCloseJobTarget(schedule));
		}

		scheduleRepository.deleteByPerformanceId(performanceId);
		castRepository.deleteByPerformanceId(performanceId);
		staffRepository.deleteByPerformanceId(performanceId);
		promotionRepository.deleteByPerformanceId(performanceId);
		performanceImageRepository.deleteByPerformanceId(performanceId);
		performanceRepository.deleteById(performance.getId());
	}
}
