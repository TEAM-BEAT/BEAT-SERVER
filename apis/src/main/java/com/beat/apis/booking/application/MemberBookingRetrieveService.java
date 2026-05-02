package com.beat.apis.booking.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.beat.apis.booking.application.dto.MemberBookingRetrieveResponse;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.service.ScheduleDomainService;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode;
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode;
import com.beat.apis.user.application.exception.UserApplicationErrorCode;
import com.beat.apis.common.application.ApiEnumMapper;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.performance.application.dto.BankNameType;

@Service
@RequiredArgsConstructor
public class MemberBookingRetrieveService {

	private final BookingRepository bookingRepository;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final PerformanceRepository performanceRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleDomainService scheduleDomainService = new ScheduleDomainService();

	public List<MemberBookingRetrieveResponse> findMemberBookings(Long memberId) {
		Member member = memberRepository.findById(memberId).orElseThrow(
			() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		Users user = userRepository.findById(member.getUserId()).orElseThrow(
			() -> new NotFoundException(UserApplicationErrorCode.USER_NOT_FOUND)
		);

		List<Booking> bookings = bookingRepository.findByUserId(user.getId());
		Map<Long, Schedule> scheduleMap = findSchedulesByBookingScheduleIds(bookings);
		Map<Long, Performance> performanceMap = findPerformancesBySchedules(scheduleMap.values());
		LocalDate today = LocalDate.now();

		return bookings.stream()
			.map(booking -> toMemberBookingResponse(today, booking, scheduleMap, performanceMap))
			.collect(Collectors.toList());
	}

	private Map<Long, Schedule> findSchedulesByBookingScheduleIds(List<Booking> bookings) {
		List<Long> scheduleIds = bookings.stream()
			.map(Booking::getScheduleId)
			.distinct()
			.toList();

		return scheduleRepository.findAllById(scheduleIds).stream()
			.collect(Collectors.toMap(Schedule::getId, Function.identity()));
	}

	private Map<Long, Performance> findPerformancesBySchedules(Collection<Schedule> schedules) {
		List<Long> performanceIds = schedules.stream()
			.map(Schedule::getPerformanceId)
			.distinct()
			.toList();

		return performanceRepository.findAllById(performanceIds).stream()
			.collect(Collectors.toMap(Performance::getId, Function.identity()));
	}

	private MemberBookingRetrieveResponse toMemberBookingResponse(LocalDate today, Booking booking, Map<Long, Schedule> scheduleMap,
		Map<Long, Performance> performanceMap) {
		Schedule schedule = findScheduleForBooking(scheduleMap, booking);
		Performance performance = findPerformanceForSchedule(performanceMap, schedule);
		int totalPaymentAmount = booking.getPurchaseTicketCount() * performance.getTicketPrice();

		return MemberBookingRetrieveResponse.of(
			booking.getUserId(),
			booking.getId(),
			schedule.getId(),
			performance.getId(),
			performance.getPerformanceTitle(),
			schedule.getPerformanceDate(),
			performance.getPerformanceVenue(),
			booking.getPurchaseTicketCount(),
			ApiEnumMapper.fromDomain(schedule.getScheduleNumber(), ScheduleNumberType.class),
			booking.getBookerName(),
			performance.getPerformanceContact(),
			ApiEnumMapper.fromDomain(performance.getBankName(), BankNameType.class),
			performance.getAccountNumber(),
			performance.getAccountHolder(),
			scheduleDomainService.calculateDueDate(today, schedule),
			ApiEnumMapper.fromDomain(booking.getBookingStatus(), BookingStatusType.class),
			booking.getCreatedAt(),
			performance.getPosterImage(),
			totalPaymentAmount
		);
	}

	private Schedule findScheduleForBooking(Map<Long, Schedule> scheduleMap, Booking booking) {
		Schedule schedule = scheduleMap.get(booking.getScheduleId());
		if (schedule == null) {
			throw new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND);
		}
		return schedule;
	}

	private Performance findPerformanceForSchedule(Map<Long, Performance> performanceMap, Schedule schedule) {
		Performance performance = performanceMap.get(schedule.getPerformanceId());
		if (performance == null) {
			throw new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND);
		}
		return performance;
	}

}
