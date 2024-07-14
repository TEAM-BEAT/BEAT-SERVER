package com.beat.domain.booking.application;

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
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberBookingService {

    private final ScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional(timeout = 200)
    @Retryable(
            value = {PessimisticLockException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000))
    public MemberBookingResponse createMemberBooking(Long memberId, MemberBookingRequest memberBookingRequest) {
        Schedule schedule = scheduleRepository.lockById(memberBookingRequest.scheduleId())
                .orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));

        int availableTicketCount = schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
        if (availableTicketCount < memberBookingRequest.purchaseTicketCount()) {
            throw new BadRequestException(ScheduleErrorCode.INSUFFICIENT_TICKETS);
        }

        schedule.setSoldTicketCount(schedule.getSoldTicketCount() + memberBookingRequest.purchaseTicketCount());

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        Booking booking = Booking.create(
                memberBookingRequest.purchaseTicketCount(),
                memberBookingRequest.bookerName(),
                memberBookingRequest.bookerPhoneNumber(),
                memberBookingRequest.isPaymentCompleted(),
                null,
                null,
                schedule,
                user
        );
        bookingRepository.save(booking);
        scheduleRepository.save(schedule);

        return MemberBookingResponse.of(
                booking.getId(),
                schedule.getId(),
                member.getId(),
                booking.getPurchaseTicketCount(),
                schedule.getScheduleNumber().getDisplayName(),
                booking.getBookerName(),
                booking.getBookerPhoneNumber(),
                booking.isPaymentCompleted(),
                schedule.getPerformance().getBankName().name(),
                schedule.getPerformance().getAccountNumber(),
                memberBookingRequest.totalPaymentAmount(),
                booking.getCreatedAt()
        );
    }
}