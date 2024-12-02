package com.beat.domain.booking.application;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.nurigo.java_sdk.exceptions.CoolsmsException;

import com.beat.domain.booking.application.dto.TicketDeleteRequest;
import com.beat.domain.booking.application.dto.TicketDetail;
import com.beat.domain.booking.application.dto.TicketRefundRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateDetail;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.dao.TicketRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.booking.exception.TicketErrorCode;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

	private final TicketRepository ticketRepository;
	private final PerformanceRepository performanceRepository;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final ScheduleRepository scheduleRepository;
	private final CoolSmsService coolSmsService;

	public TicketRetrieveResponse findAllTicketsByConditions(Long memberId, Long performanceId,
		List<ScheduleNumber> scheduleNumbers, List<BookingStatus> bookingStatuses) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(performanceId);
		performance.validatePerformanceOwnership(user.getId());

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		int totalPerformanceTicketCount = calculateTotalTicketCount(schedules);
		int totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules);

		List<Booking> bookings = ticketRepository.findBookings(performanceId, scheduleNumbers, bookingStatuses);

		return findTicketRetrieveResponse(performance, totalPerformanceTicketCount, totalPerformanceSoldTicketCount,
			bookings);
	}

	public TicketRetrieveResponse searchAllTicketsByConditions(Long memberId, Long performanceId, String searchWord,
		List<ScheduleNumber> scheduleNumbers, List<BookingStatus> bookingStatuses) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(performanceId);
		performance.validatePerformanceOwnership(user.getId());

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		int totalPerformanceTicketCount = calculateTotalTicketCount(schedules);
		int totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules);

		List<String> scheduleNumberStrings = schedules.stream()
			.map(schedule -> schedule.getScheduleNumber().name())
			.toList();

		List<String> bookingStatusStrings = Arrays.asList(
			BookingStatus.CHECKING_PAYMENT.name(),
			BookingStatus.BOOKING_CONFIRMED.name(),
			BookingStatus.BOOKING_CANCELLED.name(),
			BookingStatus.REFUND_REQUESTED.name()
		);

		if (scheduleNumbers != null && !scheduleNumbers.isEmpty()) {
			scheduleNumberStrings = scheduleNumbers.stream()
				.map(Enum::name)
				.toList();
		}

		if (bookingStatuses != null && !bookingStatuses.isEmpty()) {
			bookingStatusStrings = bookingStatuses.stream()
				.map(Enum::name)
				.toList();
		}

		List<Booking> bookings = ticketRepository.searchBookings(
			performanceId,
			searchWord,
			scheduleNumberStrings,
			bookingStatusStrings
		);

		log.info("searchTickets result: {}", bookings);

		return findTicketRetrieveResponse(performance, totalPerformanceTicketCount, totalPerformanceSoldTicketCount,
			bookings);
	}

	@NotNull
	private TicketRetrieveResponse findTicketRetrieveResponse(Performance performance, int totalPerformanceTicketCount,
		int totalPerformanceSoldTicketCount, List<Booking> bookings) {
		List<TicketDetail> bookingList = bookings.stream()
			.map(booking -> TicketDetail.of(
				booking.getId(),
				booking.getBookerName(),
				booking.getBookerPhoneNumber(),
				booking.getSchedule().getId(),
				booking.getPurchaseTicketCount(),
				booking.getCreatedAt(),
				booking.getBookingStatus(),
				booking.getSchedule().getScheduleNumber().name(),
				Optional.ofNullable(booking.getBankName()).map(BankName::name).orElse(BankName.NONE.getDisplayName()),
				booking.getAccountNumber(),
				booking.getAccountHolder()
			))
			.collect(Collectors.toList());
		log.info("Converted TicketDetail list: {}", bookingList);

		return TicketRetrieveResponse.of(
			performance.getPerformanceTitle(),
			performance.getPerformanceTeamName(),
			performance.getTotalScheduleCount(),
			totalPerformanceTicketCount,
			totalPerformanceSoldTicketCount,
			bookingList
		);
	}

	@Transactional
	public void updateTickets(Long memberId, TicketUpdateRequest request) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(request.performanceId());
		performance.validatePerformanceOwnership(user.getId());

		for (TicketUpdateDetail detail : request.bookingList()) {
			Booking booking = ticketRepository.findById(detail.bookingId())
				.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

			if (booking.getBookingStatus() == BookingStatus.BOOKING_CONFIRMED
				&& detail.bookingStatus() != BookingStatus.BOOKING_CONFIRMED) {
				throw new BadRequestException(TicketErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED);
			}

			if (booking.getBookingStatus() == BookingStatus.CHECKING_PAYMENT
				&& detail.bookingStatus() == BookingStatus.BOOKING_CONFIRMED) {
				booking.updateBookingStatus(BookingStatus.BOOKING_CONFIRMED);
				ticketRepository.save(booking);

				String message = String.format("[BEAT] %s님 %s 예매 확정되었습니다.", detail.bookerName(),
					request.performanceTitle());
				try {
					coolSmsService.sendSms(detail.bookerPhoneNumber(), message);
				} catch (CoolsmsException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Transactional
	public void refundTicketsByBookingIds(Long memberId, TicketRefundRequest ticketRefundRequest) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(ticketRefundRequest.performanceId());
		performance.validatePerformanceOwnership(user.getId());

		for (TicketRefundRequest.Booking bookingRequest : ticketRefundRequest.bookingList()) {
			Long bookingId = bookingRequest.bookingId();
			Booking booking = ticketRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

			booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED);
			ticketRepository.save(booking);

			Schedule schedule = booking.getSchedule();
			schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());

			if (!schedule.isBooking()) {
				schedule.updateIsBooking(true);
				scheduleRepository.save(schedule);
			}
		}
	}

	@Transactional
	public void deleteTicketsByBookingIds(Long memberId, TicketDeleteRequest ticketDeleteRequest) {
		Member member = findMember(memberId);
		Long userId = findUser(member).getId();
		Performance performance = findPerformance(ticketDeleteRequest.performanceId());
		performance.validatePerformanceOwnership(userId);

		if (!performance.getUsers().getId().equals(userId)) {
			throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
		}

		for (TicketDeleteRequest.Booking bookingRequest : ticketDeleteRequest.bookingList()) {
			Long bookingId = bookingRequest.bookingId();
			Booking booking = ticketRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

			booking.updateBookingStatus(BookingStatus.BOOKING_DELETED);
			ticketRepository.save(booking);

			Schedule schedule = booking.getSchedule();
			schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());

			if (!schedule.isBooking()) {
				schedule.updateIsBooking(true);
				scheduleRepository.save(schedule);
			}
		}
	}

	@Scheduled(cron = "0 0 4 * * ?")
	@Transactional
	public void deleteOldCancelledBookings() {
		LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
		List<Booking> oldCancelledBookings = ticketRepository.findByBookingStatusAndCancellationDateBefore(
			BookingStatus.BOOKING_CANCELLED, oneYearAgo);
		ticketRepository.deleteAll(oldCancelledBookings);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private Users findUser(Member member) {
		return userRepository.findById(member.getUser().getId()).orElseThrow(
			() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
	}

	private Performance findPerformance(Long performanceId) {
		return performanceRepository.findById(performanceId).orElseThrow(
			() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)
		);
	}

	private int calculateTotalTicketCount(List<Schedule> schedules) {
		return schedules.stream()
			.mapToInt(Schedule::getTotalTicketCount)
			.sum();
	}

	private int calculateTotalSoldTicketCount(List<Schedule> schedules) {
		return schedules.stream()
			.mapToInt(Schedule::getSoldTicketCount)
			.sum();
	}

}
