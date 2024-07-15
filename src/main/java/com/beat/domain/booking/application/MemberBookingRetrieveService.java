package com.beat.domain.booking.application;

import com.beat.domain.booking.application.dto.MemberBookingRetrieveResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.exception.UserErrorCode;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberBookingRetrieveService {

    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public List<MemberBookingRetrieveResponse> findMemberBookings(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        Users user = userRepository.findById(member.getUser().getId()).orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        List<Booking> bookings = bookingRepository.findByUsersId(user.getId());

        return bookings.stream()
                .map(this::toMemberBookingResponse)
                .collect(Collectors.toList());
    }

    private MemberBookingRetrieveResponse toMemberBookingResponse(Booking booking) {
        Schedule schedule = booking.getSchedule();
        Performance performance = schedule.getPerformance();

        return MemberBookingRetrieveResponse.of(
                booking.getUsers().getId(),
                booking.getId(),
                schedule.getId(),
                performance.getPerformanceTitle(),
                schedule.getPerformanceDate(),
                performance.getPerformanceVenue(),
                booking.getPurchaseTicketCount(),
                schedule.getScheduleNumber().name(),
                booking.getBookerName(),
                booking.getBookerPhoneNumber(),
                performance.getBankName().name(),
                performance.getAccountNumber(),
                schedule.getId().intValue(),
                booking.isPaymentCompleted(),
                booking.getCreatedAt()
        );
    }
}