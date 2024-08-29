package com.beat.domain.booking.application;

import com.beat.domain.booking.application.dto.*;
import com.beat.domain.booking.dao.TicketRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.TicketErrorCode;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final PerformanceRepository performanceRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final CoolSmsService coolSmsService;

    public TicketRetrieveResponse getTickets(Long memberId, Long performanceId, ScheduleNumber scheduleNumber, BookingStatus bookingStatus) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        List<Booking> bookings;

        if (scheduleNumber != null && bookingStatus != null) {
            bookings = ticketRepository.findBySchedulePerformanceIdAndScheduleScheduleNumberAndBookingStatus(performanceId, scheduleNumber, bookingStatus);
        } else if (scheduleNumber != null) {
            bookings = ticketRepository.findBySchedulePerformanceIdAndScheduleScheduleNumber(performanceId, scheduleNumber);
        } else if (bookingStatus != null) {
            bookings = ticketRepository.findBySchedulePerformanceIdAndBookingStatus(performanceId, bookingStatus);
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
                        booking.getBookingStatus(),
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

            if (booking.getBookingStatus() == BookingStatus.BOOKING_CONFIRMED && detail.bookingStatus() != BookingStatus.BOOKING_CONFIRMED) {
                throw new BadRequestException(TicketErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED);
            }

            if (booking.getBookingStatus() == BookingStatus.CHECKING_PAYMENT && detail.bookingStatus() == BookingStatus.BOOKING_CONFIRMED) {
                booking.setBookingStatus(BookingStatus.BOOKING_CONFIRMED);
                ticketRepository.save(booking);

                String message = String.format("[BEAT] %s님 %s 예매 확정되었습니다.", detail.bookerName(), request.performanceTitle());
                try {
                    coolSmsService.sendSms(detail.bookerPhoneNumber(), message);
                } catch (CoolsmsException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Transactional
    public void cancelTickets(Long memberId, TicketCancelRequest ticketCancelRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Long userId = member.getUser().getId();

        Performance performance = performanceRepository.findById(ticketCancelRequest.performanceId())
                .orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_PERFORMANCE_FOUND));

        if (!performance.getUsers().getId().equals(userId)) {
            throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
        }

        for (Long bookingId : ticketCancelRequest.bookingList()) {
            Booking booking = ticketRepository.findById(bookingId)
                    .orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

            booking.setBookingStatus(BookingStatus.BOOKING_CANCELLED);
            ticketRepository.save(booking);

            Schedule schedule = booking.getSchedule();
            schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());
            scheduleRepository.save(schedule);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteOldCancelledBookings() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<Booking> oldCancelledBookings = ticketRepository.findByBookingStatusAndCancellationDateBefore(BookingStatus.BOOKING_CANCELLED, oneYearAgo);
        ticketRepository.deleteAll(oldCancelledBookings);
    }
}
