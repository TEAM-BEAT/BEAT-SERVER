package com.beat.apis.performance.application;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailResponse;
import com.beat.apis.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailScheduleResponse;
import com.beat.apis.performance.application.dto.create.CastResponse;
import com.beat.apis.performance.application.dto.create.PerformanceImageResponse;
import com.beat.apis.performance.application.dto.create.ScheduleResponse;
import com.beat.apis.performance.application.dto.create.StaffResponse;
import com.beat.apis.performance.application.dto.makerPerformance.MakerPerformanceDetailResponse;
import com.beat.apis.performance.application.dto.makerPerformance.MakerPerformanceResponse;
import com.beat.apis.performance.application.dto.modify.PerformanceModifyDetailResponse;
import com.beat.apis.performance.application.dto.performanceDetail.PerformanceDetailCastResponse;
import com.beat.apis.performance.application.dto.performanceDetail.PerformanceDetailImageResponse;
import com.beat.apis.performance.application.dto.performanceDetail.PerformanceDetailResponse;
import com.beat.apis.performance.application.dto.performanceDetail.PerformanceDetailScheduleResponse;
import com.beat.apis.performance.application.dto.performanceDetail.PerformanceDetailStaffResponse;
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
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.service.ScheduleDomainService;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.repository.StaffRepository;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode;
import com.beat.apis.user.application.exception.UserApplicationErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {
	private final PerformanceRepository performanceRepository;
	private final ScheduleRepository scheduleRepository;
	private final CastRepository castRepository;
	private final StaffRepository staffRepository;
	private final ScheduleDomainService scheduleDomainService = new ScheduleDomainService();
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final BookingRepository bookingRepository;
	private final PerformanceImageRepository performanceImageRepository;

	@Transactional(readOnly = true)
	public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND));

		LocalDate today = LocalDate.now();
		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		List<PerformanceDetailScheduleResponse> scheduleList = schedules.stream()
			.map(schedule -> {
				int dueDate = scheduleDomainService.calculateDueDate(today, schedule);
				return PerformanceDetailScheduleResponse.of(schedule.getId(), schedule.getPerformanceDate(),
					schedule.getScheduleNumber().name(), dueDate, schedule.isBooking());
			})
			.toList();

		int minDueDate = scheduleDomainService.getMinDueDate(today, schedules);

		List<PerformanceDetailCastResponse> castList = castRepository.findAllByPerformanceId(performanceId)
			.stream()
			.map(cast -> PerformanceDetailCastResponse.of(cast.getId(), cast.getCastName(), cast.getCastRole(),
				cast.getCastPhoto()))
			.toList();

		List<PerformanceDetailStaffResponse> staffList = staffRepository.findAllByPerformanceId(performanceId)
			.stream()
			.map(staff -> PerformanceDetailStaffResponse.of(staff.getId(), staff.getStaffName(), staff.getStaffRole(),
				staff.getStaffPhoto()))
			.toList();

		List<PerformanceDetailImageResponse> performanceImageList = performanceImageRepository.findAllByPerformanceId(
				performanceId)
			.stream()
			.map(image -> PerformanceDetailImageResponse.of(image.getId(), image.getPerformanceImageUrl()))
			.toList();

		log.info("Successfully completed getPerformanceDetail for performanceId: {}", performanceId);
		return PerformanceDetailResponse.of(performance.getId(), performance.getPerformanceTitle(),
			performance.getPerformancePeriod(), scheduleList, performance.getTicketPrice(),
			performance.getGenre().name(), performance.getPosterImage(), performance.getRunningTime(),
			performance.getPerformanceVenue(), performance.getRoadAddressName(), performance.getPlaceDetailAddress(),
			performance.getLatitude(), performance.getLongitude(), performance.getPerformanceDescription(),
			performance.getPerformanceAttentionNote(), performance.getPerformanceContact(),
			performance.getPerformanceTeamName(), castList, staffList, minDueDate, performanceImageList);
	}

	@Transactional(readOnly = true)
	public BookingPerformanceDetailResponse getBookingPerformanceDetail(Long performanceId) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND));

		LocalDate today = LocalDate.now();
		List<BookingPerformanceDetailScheduleResponse> scheduleList = scheduleRepository.findAllByPerformanceId(
			performanceId).stream().map(schedule -> {
			int dueDate = scheduleDomainService.calculateDueDate(today, schedule);
			return BookingPerformanceDetailScheduleResponse.of(schedule.getId(), schedule.getPerformanceDate(),
				schedule.getScheduleNumber().name(), scheduleDomainService.getAvailableTicketCount(schedule),
				schedule.isBooking(), dueDate);
		}).toList();

		return BookingPerformanceDetailResponse.of(performance.getId(), performance.getPerformanceTitle(),
			performance.getPerformancePeriod(), scheduleList, performance.getTicketPrice(),
			performance.getGenre().name(), performance.getPosterImage(), performance.getPerformanceVenue(),
			performance.getPerformanceTeamName(),
			performance.getBankName() != null ? performance.getBankName().name() : null, performance.getAccountNumber(),
			performance.getAccountHolder());
	}

	@Transactional(readOnly = true)
	public MakerPerformanceResponse getMemberPerformances(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		userRepository.findById(member.getUserId())
			.orElseThrow(() -> new NotFoundException(UserApplicationErrorCode.USER_NOT_FOUND));

		List<Performance> performances = performanceRepository.findByUserId(member.getUserId());
		LocalDate today = LocalDate.now();

		List<MakerPerformanceDetailResponse> performanceDetails = performances.stream().map(performance -> {
			List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performance.getId());
			int minDueDate = scheduleDomainService.getMinDueDate(today, schedules);

			return MakerPerformanceDetailResponse.of(performance.getId(), performance.getGenre().name(),
				performance.getPerformanceTitle(), performance.getPosterImage(), performance.getPerformancePeriod(),
				minDueDate);
		}).toList();

		List<MakerPerformanceDetailResponse> positiveDueDates = performanceDetails.stream()
			.filter(detail -> detail.minDueDate() >= 0)
			.sorted(Comparator.comparingInt(MakerPerformanceDetailResponse::minDueDate))
			.collect(Collectors.toList());

		List<MakerPerformanceDetailResponse> negativeDueDates = performanceDetails.stream()
			.filter(detail -> detail.minDueDate() < 0)
			.sorted(Comparator.comparingInt(MakerPerformanceDetailResponse::minDueDate).reversed())
			.toList();

		positiveDueDates.addAll(negativeDueDates);

		return MakerPerformanceResponse.of(member.getUserId(), positiveDueDates);
	}

	@Transactional(readOnly = true)
	public Performance findById(Long performanceId) {
		return performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND));
	}

	@Transactional
	public PerformanceModifyDetailResponse getPerformanceEdit(Long memberId, Long performanceId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		Long userId = member.getUserId();

		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND));

		if (!Objects.equals(performance.getUserId(), userId)) {
			throw new ForbiddenException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER);
		}

		List<Long> scheduleIds = scheduleRepository.findIdsByPerformanceId(performanceId);

		List<BookingStatus> statusesToExclude = List.of(BookingStatus.BOOKING_CANCELLED, BookingStatus.BOOKING_DELETED);
		boolean isBookerExist = bookingRepository.existsActiveBookingByScheduleIds(scheduleIds, statusesToExclude);

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		List<Cast> casts = castRepository.findAllByPerformanceId(performanceId);
		List<Staff> staffs = staffRepository.findAllByPerformanceId(performanceId);
		List<PerformanceImage> performanceImages = performanceImageRepository.findAllByPerformanceId(performanceId);

		return mapToPerformanceEditResponse(performance, schedules, casts, staffs, performanceImages, isBookerExist);
	}

	private PerformanceModifyDetailResponse mapToPerformanceEditResponse(Performance performance,
		List<Schedule> schedules, List<Cast> casts, List<Staff> staffs, List<PerformanceImage> performanceImages,
		boolean isBookerExist) {
		LocalDate today = LocalDate.now();
		List<ScheduleResponse> scheduleResponses = schedules.stream()
			.map(schedule -> ScheduleResponse.of(schedule.getId(), schedule.getPerformanceDate(),
				schedule.getTotalTicketCount(),
				scheduleDomainService.calculateDueDate(today, schedule),
				schedule.getScheduleNumber()))
			.toList();

		List<CastResponse> castResponses = casts.stream()
			.map(cast -> CastResponse.of(cast.getId(), cast.getCastName(), cast.getCastRole(), cast.getCastPhoto()))
			.toList();

		List<StaffResponse> staffResponses = staffs.stream()
			.map(staff -> StaffResponse.of(staff.getId(), staff.getStaffName(), staff.getStaffRole(),
				staff.getStaffPhoto()))
			.toList();

		List<PerformanceImageResponse> performanceImageResponses = performanceImages.stream()
			.map(performanceImage -> PerformanceImageResponse.of(performanceImage.getId(),
				performanceImage.getPerformanceImageUrl()))
			.toList();

		return PerformanceModifyDetailResponse.of(performance.getUserId(), performance.getId(),
			performance.getPerformanceTitle(), performance.getGenre(), performance.getRunningTime(),
			performance.getPerformanceDescription(), performance.getPerformanceAttentionNote(),
			performance.getBankName(), performance.getAccountNumber(), performance.getAccountHolder(),
			performance.getPosterImage(), performance.getPerformanceTeamName(), performance.getPerformanceVenue(),
			performance.getRoadAddressName(), performance.getPlaceDetailAddress(), performance.getLatitude(),
			performance.getLongitude(),
			performance.getPerformanceContact(), performance.getPerformancePeriod(), performance.getTicketPrice(),
			performance.getTotalScheduleCount(), isBookerExist, scheduleResponses, castResponses, staffResponses,
			performanceImageResponses);
	}

}
