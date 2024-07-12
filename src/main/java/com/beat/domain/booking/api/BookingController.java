package com.beat.domain.booking.api;

import com.beat.domain.booking.application.BookingRetrieveService;
import com.beat.domain.booking.application.GuestBookingService;
import com.beat.domain.booking.application.dto.BookingRetrieveRequest;
import com.beat.domain.booking.application.dto.BookingRetrieveResponse;
import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.booking.exception.BookingSuccessCode;
import com.beat.global.common.dto.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRetrieveService bookingRetrieveService;
    private final GuestBookingService guestBookingService;

    @PostMapping("/guest")
    public ResponseEntity<SuccessResponse<GuestBookingResponse>> createGuestBookings(
            @RequestBody GuestBookingRequest guestBookingRequest) {
        GuestBookingResponse response = guestBookingService.createGuestBooking(guestBookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.of(BookingSuccessCode.BOOKING_SUCCESS, response));
    }

    @PostMapping("/guest/retrieve")
    public ResponseEntity<SuccessResponse<List<BookingRetrieveResponse>>> getGuestBookings(
            @RequestBody BookingRetrieveRequest bookingRetrieveRequest) {
        List<BookingRetrieveResponse> response = bookingRetrieveService.findGuestBookings(bookingRetrieveRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(BookingSuccessCode.BOOKING_RETRIEVE_SUCCESS, response));
    }
}
