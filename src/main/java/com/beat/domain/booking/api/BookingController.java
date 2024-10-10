package com.beat.domain.booking.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beat.domain.booking.application.GuestBookingRetrieveService;
import com.beat.domain.booking.application.GuestBookingService;
import com.beat.domain.booking.application.MemberBookingRetrieveService;
import com.beat.domain.booking.application.MemberBookingService;
import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.domain.booking.application.dto.MemberBookingRequest;
import com.beat.domain.booking.application.dto.MemberBookingResponse;
import com.beat.domain.booking.application.dto.MemberBookingRetrieveResponse;
import com.beat.domain.booking.exception.BookingSuccessCode;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController implements BookingApi {
	private final MemberBookingService memberBookingService;
	private final MemberBookingRetrieveService memberBookingRetrieveService;
	private final GuestBookingService guestBookingService;
	private final GuestBookingRetrieveService guestBookingRetrieveService;

	@Override
	@PostMapping("/member")
	public ResponseEntity<SuccessResponse<MemberBookingResponse>> createMemberBooking(
		@CurrentMember Long memberId,
		@RequestBody MemberBookingRequest memberBookingRequest) {
		MemberBookingResponse response = memberBookingService.createMemberBooking(memberId, memberBookingRequest);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(SuccessResponse.of(BookingSuccessCode.MEMBER_BOOKING_SUCCESS, response));
	}

	@Override
	@GetMapping("/member/retrieve")
	public ResponseEntity<SuccessResponse<List<MemberBookingRetrieveResponse>>> getMemberBookings(
		@CurrentMember Long memberId) {
		List<MemberBookingRetrieveResponse> response = memberBookingRetrieveService.findMemberBookings(memberId);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(BookingSuccessCode.MEMBER_BOOKING_RETRIEVE_SUCCESS, response));
	}

	@Override
	@PostMapping("/guest")
	public ResponseEntity<SuccessResponse<GuestBookingResponse>> createGuestBookings(
		@RequestBody GuestBookingRequest guestBookingRequest) {
		GuestBookingResponse response = guestBookingService.createGuestBooking(guestBookingRequest);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(SuccessResponse.of(BookingSuccessCode.GUEST_BOOKING_SUCCESS, response));
	}

	@Override
	@PostMapping("/guest/retrieve")
	public ResponseEntity<SuccessResponse<List<GuestBookingRetrieveResponse>>> getGuestBookings(
		@RequestBody GuestBookingRetrieveRequest guestBookingRetrieveRequest) {
		List<GuestBookingRetrieveResponse> response = guestBookingRetrieveService.findGuestBookings(
			guestBookingRetrieveRequest);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(BookingSuccessCode.GUEST_BOOKING_RETRIEVE_SUCCESS, response));
	}
}
