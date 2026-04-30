package com.beat.apis.booking.application;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.booking.application.dto.TicketDeleteRequest;
import com.beat.apis.booking.application.dto.TicketDetail;
import com.beat.apis.booking.application.dto.TicketRefundRequest;
import com.beat.apis.booking.application.dto.TicketRetrieveResponse;
import com.beat.apis.booking.application.dto.TicketUpdateDetail;
import com.beat.apis.booking.application.dto.TicketUpdateRequest;
import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;
import com.beat.contracts.sms.SmsMessage;
import com.beat.contracts.sms.SmsPort;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.booking.exception.TicketErrorCode;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

	private final BookingRepository bookingRepository;
	private final MakerTicketReadPort makerTicketReadPort;
	private final PerformanceRepository performanceRepository;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final ScheduleRepository scheduleRepository;
	private final SmsPort smsPort;

	public TicketRetrieveResponse findAllTicketsByConditions(Long memberId, Long performanceId,
		List<ScheduleNumber> scheduleNumbers, List<BookingStatus> bookingStatuses) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(performanceId);
		performance.validatePerformanceOwnership(user.getId());

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		int totalPerformanceTicketCount = calculateTotalTicketCount(schedules);
		int totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules);

		log.info("performanceId: {}", performanceId);
		log.info("scheduleNumbers: {}", scheduleNumbers);
		log.info("bookingStatuses: {}", bookingStatuses);
		List<MakerTicketListItemReadModel> tickets = makerTicketReadPort.findTickets(
			performanceId,
			toNames(scheduleNumbers),
			toNames(bookingStatuses)
		);

		return findTicketRetrieveResponse(performance, totalPerformanceTicketCount, totalPerformanceSoldTicketCount,
			schedules, tickets);
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

		List<String> selectedScheduleNumbers = schedules.stream()
			.map(schedule -> schedule.getScheduleNumber().name())
			.toList();

		List<String> selectedBookingStatuses = Arrays.asList(
			BookingStatus.REFUND_REQUESTED.name(),
			BookingStatus.CHECKING_PAYMENT.name(),
			BookingStatus.BOOKING_CONFIRMED.name(),
			BookingStatus.BOOKING_CANCELLED.name()
		);

		if (scheduleNumbers != null && !scheduleNumbers.isEmpty()) {
			selectedScheduleNumbers = scheduleNumbers.stream()
				.map(Enum::name)
				.toList();
		}

		if (bookingStatuses != null && !bookingStatuses.isEmpty()) {
			selectedBookingStatuses = bookingStatuses.stream()
				.map(Enum::name)
				.toList();
		}

		log.info("performanceId: {}", performanceId);
		log.info("searchWord: {}", searchWord);
		log.info("selectedScheduleNumbers: {}", selectedScheduleNumbers);
		log.info("selectedBookingStatuses: {}", selectedBookingStatuses);
		List<MakerTicketListItemReadModel> tickets = makerTicketReadPort.searchTickets(
			performanceId,
			searchWord,
			selectedScheduleNumbers,
			selectedBookingStatuses
		);

		log.info("searchTickets result: {}", tickets);

		return findTicketRetrieveResponse(performance, totalPerformanceTicketCount, totalPerformanceSoldTicketCount,
			schedules, tickets);
	}

	private <E extends Enum<E>> List<String> toNames(List<E> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
			.map(Enum::name)
			.toList();
	}

	@NotNull
	private TicketRetrieveResponse findTicketRetrieveResponse(Performance performance, int totalPerformanceTicketCount,
		int totalPerformanceSoldTicketCount, List<Schedule> schedules, List<MakerTicketListItemReadModel> tickets) {
		Map<Long, Schedule> scheduleMap = schedules.stream()
			.collect(Collectors.toMap(s -> s.getId(), s -> s));
		List<TicketDetail> bookingList = tickets.stream()
			.map(ticket -> {
				Schedule schedule = findScheduleForTicket(scheduleMap, ticket);
				return TicketDetail.of(
					ticket.bookingId(),
					ticket.bookerName(),
					ticket.bookerPhoneNumber(),
					ticket.scheduleId(),
					ticket.purchaseTicketCount(),
					ticket.createdAt(),
					BookingStatus.valueOf(ticket.bookingStatus()),
					schedule.getScheduleNumber().name(),
					ticket.bankName(),
					ticket.accountNumber(),
					ticket.accountHolder()
				);
			})
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

	private Schedule findScheduleForTicket(Map<Long, Schedule> scheduleMap, MakerTicketListItemReadModel ticket) {
		Schedule schedule = scheduleMap.get(ticket.scheduleId());
		if (schedule == null) {
			throw new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
		}
		return schedule;
	}

	@Transactional
	public void updateTickets(Long memberId, TicketUpdateRequest request) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(request.performanceId());
		performance.validatePerformanceOwnership(user.getId());

		for (TicketUpdateDetail detail : request.bookingList()) {
			Booking booking = bookingRepository.findById(detail.bookingId())
				.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

			if (booking.getBookingStatus() == BookingStatus.BOOKING_CONFIRMED
				&& detail.bookingStatus() != BookingStatus.BOOKING_CONFIRMED) {
				throw new BadRequestException(TicketErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED);
			}

			if (booking.getBookingStatus() == BookingStatus.CHECKING_PAYMENT
				&& detail.bookingStatus() == BookingStatus.BOOKING_CONFIRMED) {
				booking = booking.updateBookingStatus(BookingStatus.BOOKING_CONFIRMED);
				booking = bookingRepository.save(booking);

				String message = String.format("[BEAT] %s님 %s 예매 확정되었습니다.", detail.bookerName(),
					request.performanceTitle());
				try {
					smsPort.sendSms(new SmsMessage(detail.bookerPhoneNumber(), message));
				} catch (RuntimeException e) {
					log.error("SMS 전송 실패 - 예매자: {}, 공연: {}", detail.bookerName(), request.performanceTitle(), e);
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
			Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

			booking = booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED);
			booking = bookingRepository.save(booking);

			Schedule schedule = scheduleRepository.lockById(booking.getScheduleId())
				.orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));
			Schedule updated = schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());
			if (!updated.isBooking()) {
				updated = updated.updateIsBooking(true);
			}
			scheduleRepository.save(updated);
		}
	}

	@Transactional
	public void deleteTicketsByBookingIds(Long memberId, TicketDeleteRequest ticketDeleteRequest) {
		Member member = findMember(memberId);
		Long userId = findUser(member).getId();
		Performance performance = findPerformance(ticketDeleteRequest.performanceId());
		performance.validatePerformanceOwnership(userId);

		for (TicketDeleteRequest.Booking bookingRequest : ticketDeleteRequest.bookingList()) {
			Long bookingId = bookingRequest.bookingId();
			Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

			booking = booking.updateBookingStatus(BookingStatus.BOOKING_DELETED);
			booking = bookingRepository.save(booking);

			Schedule schedule = scheduleRepository.lockById(booking.getScheduleId())
				.orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));
			Schedule updated = schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());
			if (!updated.isBooking()) {
				updated = updated.updateIsBooking(true);
			}
			scheduleRepository.save(updated);
		}
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private Users findUser(Member member) {
		return userRepository.findById(member.getUserId()).orElseThrow(
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
