package com.beat.apis.ticket.application;

import com.beat.apis.common.application.converter.BookingStatusEnumConverter;
import com.beat.apis.common.application.converter.ScheduleNumberEnumConverter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.ticket.application.dto.TicketDeleteRequest;
import com.beat.apis.ticket.application.dto.TicketDetail;
import com.beat.apis.ticket.application.dto.TicketRefundRequest;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.apis.ticket.application.dto.TicketUpdateDetail;
import com.beat.apis.ticket.application.dto.TicketUpdateRequest;
import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus;
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber;
import com.beat.contracts.sms.SmsMessage;
import com.beat.contracts.sms.SmsPort;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beat.apis.booking.application.exception.BookingApplicationErrorCode;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode;
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode;
import com.beat.apis.user.application.exception.UserApplicationErrorCode;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;

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
		List<ScheduleNumberType> scheduleNumbers, List<BookingStatusType> bookingStatuses) {
		validateDeletedTicketsAreNotRequested(bookingStatuses);

		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(performanceId);
		validatePerformanceOwnership(performance, user.getId());

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		int totalPerformanceTicketCount = calculateTotalTicketCount(schedules);
		int totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules);

		log.info("performanceId: {}", performanceId);
		log.info("scheduleNumbers: {}", scheduleNumbers);
		log.info("bookingStatuses: {}", bookingStatuses);
		List<MakerTicketListItemReadModel> tickets = makerTicketReadPort.findTickets(
			performanceId,
			toMakerTicketScheduleNumbers(scheduleNumbers),
			toMakerTicketBookingStatuses(bookingStatuses)
		);

		return findTicketRetrieveResponse(performance, totalPerformanceTicketCount, totalPerformanceSoldTicketCount,
			schedules, tickets);
	}

	public TicketRetrieveResponse searchAllTicketsByConditions(Long memberId, Long performanceId, String searchWord,
		List<ScheduleNumberType> scheduleNumbers, List<BookingStatusType> bookingStatuses) {
		validateSearchWord(searchWord);
		validateDeletedTicketsAreNotRequested(bookingStatuses);

		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(performanceId);
		validatePerformanceOwnership(performance, user.getId());

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performanceId);
		int totalPerformanceTicketCount = calculateTotalTicketCount(schedules);
		int totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules);

		List<MakerTicketScheduleNumber> selectedScheduleNumbers = schedules.stream()
			.map(schedule -> ScheduleNumberEnumConverter.toMakerTicketScheduleNumber(schedule.getScheduleNumber()))
			.toList();

		List<MakerTicketBookingStatus> selectedBookingStatuses = List.of(
			MakerTicketBookingStatus.REFUND_REQUESTED,
			MakerTicketBookingStatus.CHECKING_PAYMENT,
			MakerTicketBookingStatus.BOOKING_CONFIRMED,
			MakerTicketBookingStatus.BOOKING_CANCELLED
		);

		if (scheduleNumbers != null && !scheduleNumbers.isEmpty()) {
			selectedScheduleNumbers = toMakerTicketScheduleNumbers(scheduleNumbers);
		}

		if (bookingStatuses != null && !bookingStatuses.isEmpty()) {
			selectedBookingStatuses = toMakerTicketBookingStatuses(bookingStatuses);
		}

		log.info("Searching maker tickets: performanceId={}, scheduleFilterCount={}, statusFilterCount={}",
			performanceId, selectedScheduleNumbers.size(), selectedBookingStatuses.size());
		List<MakerTicketListItemReadModel> tickets = makerTicketReadPort.searchTickets(
			performanceId,
			searchWord,
			selectedScheduleNumbers,
			selectedBookingStatuses
		);

		log.info("searchTickets result count: {}", tickets.size());

		return findTicketRetrieveResponse(performance, totalPerformanceTicketCount, totalPerformanceSoldTicketCount,
			schedules, tickets);
	}

	private void validateSearchWord(String searchWord) {
		if (searchWord == null || searchWord.length() < 2) {
			throw new BadRequestException(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT);
		}
	}

	private void validateDeletedTicketsAreNotRequested(List<BookingStatusType> bookingStatuses) {
		if (bookingStatuses != null && bookingStatuses.contains(BookingStatusType.BOOKING_DELETED)) {
			throw new BadRequestException(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED);
		}
	}

	private List<MakerTicketScheduleNumber> toMakerTicketScheduleNumbers(List<ScheduleNumberType> scheduleNumbers) {
		if (scheduleNumbers == null || scheduleNumbers.isEmpty()) {
			return List.of();
		}
		return scheduleNumbers.stream()
			.map(ScheduleNumberEnumConverter::toMakerTicketScheduleNumber)
			.toList();
	}

	private List<MakerTicketBookingStatus> toMakerTicketBookingStatuses(List<BookingStatusType> bookingStatuses) {
		if (bookingStatuses == null || bookingStatuses.isEmpty()) {
			return List.of();
		}
		return bookingStatuses.stream()
			.map(BookingStatusEnumConverter::toMakerTicketStatus)
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
					BookingStatusEnumConverter.toApi(ticket.bookingStatus()),
					ScheduleNumberEnumConverter.toApiName(schedule.getScheduleNumber()),
					ticket.bankName(),
					ticket.accountNumber(),
					ticket.accountHolder()
				);
			})
			.collect(Collectors.toList());
		log.info("Converted TicketDetail count: {}", bookingList.size());

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
			throw new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND);
		}
		return schedule;
	}

	@Transactional
	public void updateTickets(Long memberId, TicketUpdateRequest request) {
		Member member = findMember(memberId);
		Users user = findUser(member);
		Performance performance = findPerformance(request.performanceId());
		validatePerformanceOwnership(performance, user.getId());

		for (TicketUpdateDetail detail : request.bookingList()) {
			Booking booking = bookingRepository.findById(detail.bookingId())
				.orElseThrow(() -> new NotFoundException(BookingApplicationErrorCode.NO_BOOKING_FOUND));
			BookingStatus requestedBookingStatus = BookingStatusEnumConverter.toDomainForTicketUpdate(detail.bookingStatus());

			if (booking.getBookingStatus() == BookingStatus.BOOKING_CONFIRMED
				&& requestedBookingStatus != BookingStatus.BOOKING_CONFIRMED) {
				throw new BadRequestException(TicketApplicationErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED);
			}

			if (booking.getBookingStatus() == BookingStatus.CHECKING_PAYMENT
				&& requestedBookingStatus == BookingStatus.BOOKING_CONFIRMED) {
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
		validatePerformanceOwnership(performance, user.getId());

		for (TicketRefundRequest.Booking bookingRequest : ticketRefundRequest.bookingList()) {
			Long bookingId = bookingRequest.bookingId();
			Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException(BookingApplicationErrorCode.NO_BOOKING_FOUND));

			booking = booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED);
			booking = bookingRepository.save(booking);

			Schedule schedule = scheduleRepository.lockById(booking.getScheduleId())
				.orElseThrow(() -> new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND));
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
		validatePerformanceOwnership(performance, userId);

		for (TicketDeleteRequest.Booking bookingRequest : ticketDeleteRequest.bookingList()) {
			Long bookingId = bookingRequest.bookingId();
			Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException(BookingApplicationErrorCode.NO_BOOKING_FOUND));

			booking = booking.updateBookingStatus(BookingStatus.BOOKING_DELETED);
			booking = bookingRepository.save(booking);

			Schedule schedule = scheduleRepository.lockById(booking.getScheduleId())
				.orElseThrow(() -> new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND));
			Schedule updated = schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());
			if (!updated.isBooking()) {
				updated = updated.updateIsBooking(true);
			}
			scheduleRepository.save(updated);
		}
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));
	}

	private Users findUser(Member member) {
		return userRepository.findById(member.getUserId()).orElseThrow(
			() -> new NotFoundException(UserApplicationErrorCode.USER_NOT_FOUND));
	}

	private Performance findPerformance(Long performanceId) {
		return performanceRepository.findById(performanceId).orElseThrow(
			() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND)
		);
	}

	private void validatePerformanceOwnership(Performance performance, Long userId) {
		if (!performance.isOwnedBy(userId)) {
			throw new ForbiddenException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER);
		}
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
