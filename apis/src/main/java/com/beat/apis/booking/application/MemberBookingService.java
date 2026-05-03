package com.beat.apis.booking.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.booking.application.dto.MemberBookingRequest;
import com.beat.apis.booking.application.dto.MemberBookingResponse;
import com.beat.apis.booking.application.dto.event.BookingCreatedEvent;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beat.apis.member.application.exception.MemberApplicationErrorCode;
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode;
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode;
import com.beat.apis.common.application.ApiEnumMapper;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.domain.booking.domain.BookingStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBookingService {

	private final ScheduleRepository scheduleRepository;
	private final BookingRepository bookingRepository;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final PerformanceRepository performanceRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(timeout = 200)
	public MemberBookingResponse createMemberBooking(Long memberId, MemberBookingRequest memberBookingRequest) {
		Schedule schedule = scheduleRepository.lockById(memberBookingRequest.scheduleId())
			.orElseThrow(() -> new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND));

		int availableTicketCount = schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
		if (availableTicketCount < memberBookingRequest.purchaseTicketCount()) {
			throw new BadRequestException(ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS);
		}

		schedule = updateSoldTicketCountAndIsBooking(schedule, memberBookingRequest.purchaseTicketCount());

		Performance performance = performanceRepository.findById(schedule.getPerformanceId())
			.orElseThrow(() -> new NotFoundException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND));

		Member member = memberRepository.findById(memberId).orElseThrow(
			() -> new NotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND));

		Booking booking = Booking.create(
			memberBookingRequest.purchaseTicketCount(),
			memberBookingRequest.bookerName(),
			memberBookingRequest.bookerPhoneNumber(),
			BookingStatus.CHECKING_PAYMENT,
			null,
			null,
			null,
			null,
			null,
			schedule.getId(),
			member.getUserId()
		);
		booking = bookingRepository.save(booking);
		schedule = scheduleRepository.save(schedule);

		log.info("Member Booking created: {}", booking);

		eventPublisher.publishEvent(BookingCreatedEvent.of(
			booking.getCreatedAt(),
			performance.getPerformanceTitle(),
			booking.getPurchaseTicketCount(),
			booking.getBookerName(),
			schedule.getScheduleNumber().getDisplayName(),
			schedule.getSoldTicketCount(),
			schedule.getTotalTicketCount()
		));

		return MemberBookingResponse.of(
			booking.getId(),
			schedule.getId(),
			member.getId(),
			booking.getPurchaseTicketCount(),
			ApiEnumMapper.fromDomain(schedule.getScheduleNumber(), ScheduleNumberType.class),
			booking.getBookerName(),
			booking.getBookerPhoneNumber(),
			ApiEnumMapper.fromDomain(booking.getBookingStatus(), BookingStatusType.class),
			ApiEnumMapper.fromDomain(performance.getBankName(), BankNameType.class),
			performance.getAccountNumber(),
			memberBookingRequest.totalPaymentAmount(),
			booking.getCreatedAt()
		);
	}

	private Schedule updateSoldTicketCountAndIsBooking(Schedule schedule, int purchaseTicketCount) {
		schedule = schedule.increaseSoldTicketCount(purchaseTicketCount);
		if (schedule.getTotalTicketCount() == schedule.getSoldTicketCount()) {
			schedule = schedule.updateIsBooking(false);
		}
		return schedule;
	}
}
