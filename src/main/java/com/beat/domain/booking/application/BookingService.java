package com.beat.domain.booking.application;

import com.beat.domain.booking.application.dto.BookingRetrieveRequest;
import com.beat.domain.booking.application.dto.BookingRetrieveResponse;
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
public class BookingService {

    private final BookingRepository bookingRepository;

    public List<BookingRetrieveResponse> findGuestBookings(BookingRetrieveRequest bookingRetrieveRequest) {

        validateRequest(bookingRetrieveRequest);

        List<Booking> bookings = bookingRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
                        bookingRetrieveRequest.bookerName(), bookingRetrieveRequest.bookerPhoneNumber(), bookingRetrieveRequest.password(), bookingRetrieveRequest.birthDate()).orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

        if (bookings.isEmpty()) {
            throw new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND);
        }

        return bookings.stream()
                .map(this::toBookingResponse)
                .toList();
    }

    private void validateRequest(BookingRetrieveRequest bookingRetrieveRequest) {
        if (bookingRetrieveRequest.bookerName() == null || bookingRetrieveRequest.bookerPhoneNumber() == null || bookingRetrieveRequest.password() == null || bookingRetrieveRequest.birthDate() == null) {
            throw new BadRequestException(BookingErrorCode.REQUIRED_DATA_MISSING);
        }

        if (!Pattern.matches("^[a-zA-Z가-힣]+$", bookingRetrieveRequest.bookerName())) { // 예매자 이름은 알파벳, 한글 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }

        if (!Pattern.matches("^\\d{3}-\\d{4}-\\d{4}$", bookingRetrieveRequest.bookerPhoneNumber())) { // 전화번호는 010-1234-5678 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }

        if (!Pattern.matches("^\\d{4}$", bookingRetrieveRequest.password())) { // 비밀번호는 4자리 숫자 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }

        if (!Pattern.matches("^\\d{6}$", bookingRetrieveRequest.birthDate())) { // 생년월일은 6자리 숫자 형식
            throw new BadRequestException(BookingErrorCode.INVALID_REQUEST_FORMAT);
        }
    }

    private BookingRetrieveResponse toBookingResponse(Booking booking) {
        Schedule schedule = booking.getSchedule();
        Performance performance = schedule.getPerformance();

        return BookingRetrieveResponse.of(
                booking.getBookingId(),
                schedule.getScheduleId(),
                performance.getPerformanceTitle(),
                schedule.getPerformanceDate(),
                performance.getPerformanceVenue(),
                booking.getPurchaseTicketCount(),
                schedule.getScheduleNumber().name(),
                booking.getBookerName(),
                booking.getBookerPhoneNumber(),
                performance.getBankName().name(),
                performance.getAccountNumber(),
                schedule.getScheduleId().intValue(),
                booking.isPaymentCompleted(),
                booking.getCreatedAt()
        );
    }
}