package com.beat.domain.booking.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.booking.application.dto.MemberBookingRequest;
import com.beat.domain.booking.application.dto.MemberBookingResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBookingService {

	private final ScheduleRepository scheduleRepository;
	private final BookingRepository bookingRepository;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;

	@Transactional(timeout = 200)
	public MemberBookingResponse createMemberBooking(Long memberId, MemberBookingRequest memberBookingRequest) {
		Schedule schedule = scheduleRepository.lockById(memberBookingRequest.scheduleId())
			.orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));

		int availableTicketCount = schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
		if (availableTicketCount < memberBookingRequest.purchaseTicketCount()) {
			throw new BadRequestException(ScheduleErrorCode.INSUFFICIENT_TICKETS);
		}

		updateSoldTicketCountAndIsBooking(schedule, memberBookingRequest.purchaseTicketCount());

		Member member = memberRepository.findById(memberId).orElseThrow(
			() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

		Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
			() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

		Booking booking = Booking.create(
			memberBookingRequest.purchaseTicketCount(),
			memberBookingRequest.bookerName(),
			memberBookingRequest.bookerPhoneNumber(),
			memberBookingRequest.bookingStatus(),
			null,
			null,
			null,
			null,
			null,
			schedule,
			user
		);
		bookingRepository.save(booking);
		scheduleRepository.save(schedule);

		log.info("Member Booking created: {}", booking);

		return MemberBookingResponse.of(
			booking.getId(),
			schedule.getId(),
			member.getId(),
			booking.getPurchaseTicketCount(),
			schedule.getScheduleNumber(),
			booking.getBookerName(),
			booking.getBookerPhoneNumber(),
			booking.getBookingStatus(),
			schedule.getPerformance().getBankName(),
			schedule.getPerformance().getAccountNumber(),
			memberBookingRequest.totalPaymentAmount(),
			//  비회원 예매처럼 int totalPaymentAmount = ticketPrice * guestBookingRequest.purchaseTicketCount();로 계산해서 반영하기 + 요청한 총 가격 == 티켓 가격 * 수 같은지 검증하는 로직 추가하기
			booking.getCreatedAt()
		);
	}

	private void updateSoldTicketCountAndIsBooking(Schedule schedule, int purchaseTicketCount) {
		schedule.setSoldTicketCount(schedule.getSoldTicketCount() + purchaseTicketCount);

		if (schedule.getTotalTicketCount() == schedule.getSoldTicketCount()) {
			schedule.updateIsBooking(false);
		}
	}
}