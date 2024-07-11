package com.beat.domain.booking.api;

import com.beat.domain.booking.application.BookingService;
import com.beat.domain.booking.application.dto.BookingRetrieveRequest;
import com.beat.domain.booking.application.dto.BookingRetrieveResponse;
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

    private final BookingService bookingService;

    @PostMapping("/guest/retrieve")
    public ResponseEntity<SuccessResponse<List<BookingRetrieveResponse>>> getGuestBookings(
            @RequestBody BookingRetrieveRequest bookingRetrieveRequest) {
        List<BookingRetrieveResponse> response = bookingService.findGuestBookings(bookingRetrieveRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(BookingSuccessCode.BOOKING_RETRIEVE_SUCCESS, response));
    }
}
