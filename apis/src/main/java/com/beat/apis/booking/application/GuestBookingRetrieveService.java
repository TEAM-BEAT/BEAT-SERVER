package com.beat.apis.booking.application;

import com.beat.apis.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.apis.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestBookingRetrieveService {

	private final BookingRepository bookingRepository;
	private final PerformanceRepository performanceRepository;
	private final ScheduleRepository scheduleRepository;

	public List<GuestBookingRetrieveResponse> findGuestBookings(
		GuestBookingRetrieveRequest guestBookingRetrieveRequest) {

		validateRequest(guestBookingRetrieveRequest);

		List<Booking> bookings = bookingRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
			guestBookingRetrieveRequest.bookerName(), guestBookingRetrieveRequest.bookerPhoneNumber(),
			guestBookingRetrieveRequest.password(), guestBookingRetrieveRequest.birthDate()).orElseThrow(
			() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

		if (bookings.isEmpty()) {
			throw new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND);
		}

		Map<Long, Schedule> scheduleMap = findSchedulesByBookingScheduleIds(bookings);
		Map<Long, Performance> performanceMap = findPerformancesBySchedules(scheduleMap.values());

		return bookings.stream()
			.map(booking -> toBookingResponse(booking, scheduleMap, performanceMap))
			.toList();
	}

	private void validateRequest(GuestBookingRetrieveRequest guestBookingRetrieveRequest) {
		if (guestBookingRetrieveRequest.bookerName() == null || guestBookingRetrieveRequest.bookerPhoneNumber() == null
			|| guestBookingRetrieveRequest.password() == null || guestBookingRetrieveRequest.birthDate() == null) {
			throw new BadRequestException(BookingErrorCode.REQUIRED_DATA_MISSING);
		}

		if (!Pattern.matches("^[a-zA-Z가-힣]+$", guestBookingRetrieveRequest.bookerName())) { // 예매자 이름은 알파벳, 한글 형식
			throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
		}

		if (!Pattern.matches("^\\d{3}-\\d{4}-\\d{4}$",
			guestBookingRetrieveRequest.bookerPhoneNumber())) { // 전화번호는 010-1234-5678 형식
			throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
		}

		if (!Pattern.matches("^\\d{4}$", guestBookingRetrieveRequest.password())) { // 비밀번호는 4자리 숫자 형식
			throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
		}

		if (!Pattern.matches("^\\d{6}$", guestBookingRetrieveRequest.birthDate())) { // 생년월일은 6자리 숫자 형식
			throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
		}
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

	private GuestBookingRetrieveResponse toBookingResponse(Booking booking, Map<Long, Schedule> scheduleMap,
		Map<Long, Performance> performanceMap) {
		Schedule schedule = findScheduleForBooking(scheduleMap, booking);
		Performance performance = findPerformanceForSchedule(performanceMap, schedule);
		int totalPaymentAmount = booking.getPurchaseTicketCount() * performance.getTicketPrice();

		return GuestBookingRetrieveResponse.of(
			booking.getId(),
			schedule.getId(),
			performance.getId(),
			performance.getPerformanceTitle(),
			schedule.getPerformanceDate(),
			performance.getPerformanceVenue(),
			booking.getPurchaseTicketCount(),
			schedule.getScheduleNumber(),
			booking.getBookerName(),
			performance.getPerformanceContact(),
			performance.getBankName(),
			performance.getAccountNumber(),
			performance.getAccountHolder(),
			calculateDueDate(schedule.getPerformanceDate()),
			booking.getBookingStatus(),
			booking.getCreatedAt(),
			performance.getPosterImage(),
			totalPaymentAmount
		);
	}

	private Schedule findScheduleForBooking(Map<Long, Schedule> scheduleMap, Booking booking) {
		Schedule schedule = scheduleMap.get(booking.getScheduleId());
		if (schedule == null) {
			throw new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
		}
		return schedule;
	}

	private Performance findPerformanceForSchedule(Map<Long, Performance> performanceMap, Schedule schedule) {
		Performance performance = performanceMap.get(schedule.getPerformanceId());
		if (performance == null) {
			throw new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
		}
		return performance;
	}

	private int calculateDueDate(LocalDateTime performanceDate) {
		return (int)ChronoUnit.DAYS.between(LocalDate.now(), performanceDate.toLocalDate());
	}
}
