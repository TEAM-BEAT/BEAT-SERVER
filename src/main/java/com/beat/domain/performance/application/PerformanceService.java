package com.beat.domain.performance.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailScheduleResponse;
import com.beat.domain.performance.application.dto.create.CastResponse;
import com.beat.domain.performance.application.dto.create.PerformanceImageResponse;
import com.beat.domain.performance.application.dto.create.ScheduleResponse;
import com.beat.domain.performance.application.dto.create.StaffResponse;
import com.beat.domain.performance.application.dto.makerPerformance.MakerPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.makerPerformance.MakerPerformanceResponse;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyDetailResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailCastResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailImageResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailScheduleResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailStaffResponse;
import com.beat.domain.performance.dao.PerformanceImageRepository;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.domain.PerformanceImage;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.performance.port.in.PerformanceUseCase;
import com.beat.domain.schedule.application.ScheduleService;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService implements PerformanceUseCase {
	private final PerformanceRepository performanceRepository;
	private final ScheduleRepository scheduleRepository;
	private final CastRepository castRepository;
	private final StaffRepository staffRepository;
	private final ScheduleService scheduleService;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final BookingRepository bookingRepository;
	private final PerformanceImageRepository performanceImageRepository;

	@Transactional(readOnly = true)
	public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

		List<PerformanceDetailScheduleResponse> scheduleList = scheduleRepository.findAllByPerformanceId(performanceId)
			.stream()
			.map(schedule -> {
				int dueDate = scheduleService.calculateDueDate(schedule);
				return PerformanceDetailScheduleResponse.of(schedule.getId(), schedule.getPerformanceDate(),
					schedule.getScheduleNumber().name(), dueDate, schedule.isBooking());
			})
			.toList();

		int minDueDate = scheduleService.getMinDueDate(scheduleRepository.findAllByPerformanceId(performanceId));

		List<PerformanceDetailCastResponse> castList = castRepository.findByPerformanceId(performanceId)
			.stream()
			.map(cast -> PerformanceDetailCastResponse.of(cast.getId(), cast.getCastName(), cast.getCastRole(),
				cast.getCastPhoto()))
			.toList();

		List<PerformanceDetailStaffResponse> staffList = staffRepository.findByPerformanceId(performanceId)
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
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

		List<BookingPerformanceDetailScheduleResponse> scheduleList = scheduleRepository.findAllByPerformanceId(
			performanceId).stream().map(schedule -> {
			int dueDate = scheduleService.calculateDueDate(schedule);
			return BookingPerformanceDetailScheduleResponse.of(schedule.getId(), schedule.getPerformanceDate(),
				schedule.getScheduleNumber().name(), scheduleService.getAvailableTicketCount(schedule),
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
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

		Users user = userRepository.findById(member.getUser().getId())
			.orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

		List<Performance> performances = performanceRepository.findByUsersId(user.getId());

		List<MakerPerformanceDetailResponse> performanceDetails = performances.stream().map(performance -> {
			List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performance.getId());
			int minDueDate = scheduleService.getMinDueDate(schedules);

			return MakerPerformanceDetailResponse.of(performance.getId(), performance.getGenre().name(),
				performance.getPerformanceTitle(), performance.getPosterImage(), performance.getPerformancePeriod(),
				minDueDate);
		}).toList();

		List<MakerPerformanceDetailResponse> positiveDueDates = performanceDetails.stream()
			.filter(detail -> detail.minDueDate() >= 0)
			.sorted(Comparator.comparingInt(MakerPerformanceDetailResponse::minDueDate))
			.toList();

		List<MakerPerformanceDetailResponse> negativeDueDates = performanceDetails.stream()
			.filter(detail -> detail.minDueDate() < 0)
			.sorted(Comparator.comparingInt(MakerPerformanceDetailResponse::minDueDate).reversed())
			.toList();

		positiveDueDates.addAll(negativeDueDates);

		return MakerPerformanceResponse.of(user.getId(), positiveDueDates);
	}

	@Override
	@Transactional(readOnly = true)
	public Performance findById(Long performanceId) {
		return performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
	}

	@Transactional
	public PerformanceModifyDetailResponse getPerformanceEdit(Long memberId, Long performanceId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

		Long userId = member.getUser().getId();

		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

		if (!performance.getUsers().getId().equals(userId)) {
			throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
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
		List<ScheduleResponse> scheduleResponses = schedules.stream()
			.map(schedule -> ScheduleResponse.of(schedule.getId(), schedule.getPerformanceDate(),
				schedule.getTotalTicketCount(), calculateDueDate(schedule.getPerformanceDate()),
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

		return PerformanceModifyDetailResponse.of(performance.getUsers().getId(), performance.getId(),
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

	private int calculateDueDate(LocalDateTime performanceDate) {
		return (int)ChronoUnit.DAYS.between(LocalDate.now(), performanceDate.toLocalDate());
	}
}
