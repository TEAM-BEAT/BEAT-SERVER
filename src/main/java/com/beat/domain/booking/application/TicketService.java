package com.beat.domain.booking.application;

import com.beat.domain.booking.application.dto.*;
import com.beat.domain.booking.dao.TicketRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.global.common.exception.NotFoundException;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final PerformanceRepository performanceRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CoolSmsService coolSmsService;

    public TicketRetrieveResponse getTickets(Long memberId, Long performanceId, ScheduleNumber scheduleNumber, Boolean isPaymentCompleted) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        List<Booking> bookings;

        if (scheduleNumber != null && isPaymentCompleted != null) {
            bookings = ticketRepository.findBySchedulePerformanceIdAndScheduleScheduleNumberAndIsPaymentCompleted(performanceId, scheduleNumber, isPaymentCompleted);
        } else if (scheduleNumber != null) {
            bookings = ticketRepository.findBySchedulePerformanceIdAndScheduleScheduleNumber(performanceId, scheduleNumber);
        } else if (isPaymentCompleted != null) {
            bookings = ticketRepository.findBySchedulePerformanceIdAndIsPaymentCompleted(performanceId, isPaymentCompleted);
        } else {
            bookings = ticketRepository.findBySchedulePerformanceId(performanceId);
        }

        List<TicketDetail> bookingList = bookings.stream()
                .map(booking -> TicketDetail.of(
                        booking.getId(),
                        booking.getBookerName(),
                        booking.getBookerPhoneNumber(),
                        booking.getSchedule().getId(),
                        booking.getPurchaseTicketCount(),
                        booking.getCreatedAt(),
                        booking.isPaymentCompleted(),
                        booking.getSchedule().getScheduleNumber().name()
                ))
                .collect(Collectors.toList());

        return TicketRetrieveResponse.of(
                performance.getPerformanceTitle(),
                performance.getTotalScheduleCount(),
                bookingList
        );
    }

    @Transactional
    public void updateTickets(Long memberId, TicketUpdateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        Performance performance = performanceRepository.findById(request.performanceId())
                .orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_PERFORMANCE_FOUND));

        for (TicketUpdateDetail detail : request.bookingList()) {
            Booking booking = ticketRepository.findById(detail.bookingId())
                    .orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

            boolean wasPaymentCompleted = booking.isPaymentCompleted();
            booking.setIsPaymentCompleted(detail.isPaymentCompleted());
            ticketRepository.save(booking);

            if (!wasPaymentCompleted && detail.isPaymentCompleted()) {
                String message = String.format("%s님, BEAT에서의 %s의 예매가 확정되었습니다.", detail.bookerName(), request.performanceTitle());
                try {
                    coolSmsService.sendSms(detail.bookerPhoneNumber(), message);
                } catch (CoolsmsException e) {
                    // 문자 발송 실패 시 로깅 또는 다른 처리를 추가할 수 있습니다.
                    e.printStackTrace();
                }
            }
        }
    }

    @Transactional
    public void deleteTickets(Long memberId, TicketDeleteRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        Performance performance = performanceRepository.findById(request.performanceId())
                .orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_PERFORMANCE_FOUND));

        for (Long bookingId : request.bookingList()) {
            Booking booking = ticketRepository.findById(bookingId)
                    .orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

            ticketRepository.delete(booking);
        }
    }
}
