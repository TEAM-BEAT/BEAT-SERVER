package com.beat.domain.booking.application;

import com.beat.domain.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GuestBookingRetrieveService {

    private final BookingRepository bookingRepository;

    public List<GuestBookingRetrieveResponse> findGuestBookings(GuestBookingRetrieveRequest guestBookingRetrieveRequest) {

        validateRequest(guestBookingRetrieveRequest);

        List<Booking> bookings = bookingRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
                        guestBookingRetrieveRequest.bookerName(), guestBookingRetrieveRequest.bookerPhoneNumber(), guestBookingRetrieveRequest.password(), guestBookingRetrieveRequest.birthDate()).orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

        if (bookings.isEmpty()) {
            throw new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND);
        }

        return bookings.stream()
                .map(this::toBookingResponse)
                .toList();
    }

    private void validateRequest(GuestBookingRetrieveRequest guestBookingRetrieveRequest) {
        if (guestBookingRetrieveRequest.bookerName() == null || guestBookingRetrieveRequest.bookerPhoneNumber() == null || guestBookingRetrieveRequest.password() == null || guestBookingRetrieveRequest.birthDate() == null) {
            throw new BadRequestException(BookingErrorCode.REQUIRED_DATA_MISSING);
        }

        if (!Pattern.matches("^[a-zA-Z가-힣]+$", guestBookingRetrieveRequest.bookerName())) { // 예매자 이름은 알파벳, 한글 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }

        if (!Pattern.matches("^\\d{3}-\\d{4}-\\d{4}$", guestBookingRetrieveRequest.bookerPhoneNumber())) { // 전화번호는 010-1234-5678 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }

        if (!Pattern.matches("^\\d{4}$", guestBookingRetrieveRequest.password())) { // 비밀번호는 4자리 숫자 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }

        if (!Pattern.matches("^\\d{6}$", guestBookingRetrieveRequest.birthDate())) { // 생년월일은 6자리 숫자 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }
    }

    private GuestBookingRetrieveResponse toBookingResponse(Booking booking) {
        Schedule schedule = booking.getSchedule();
        Performance performance = schedule.getPerformance();

        return GuestBookingRetrieveResponse.of(
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