package com.beat.apis.booking.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.apis.booking.application.BookingCancelService;
import com.beat.apis.booking.application.GuestBookingRetrieveService;
import com.beat.apis.booking.application.GuestBookingService;
import com.beat.apis.booking.application.MemberBookingRetrieveService;
import com.beat.apis.booking.application.MemberBookingService;
import com.beat.apis.booking.application.dto.BookingCancelRequest;
import com.beat.apis.booking.application.dto.BookingCancelResponse;
import com.beat.apis.booking.application.dto.BookingRefundRequest;
import com.beat.apis.booking.application.dto.BookingRefundResponse;
import com.beat.apis.booking.application.dto.GuestBookingRequest;
import com.beat.apis.booking.application.dto.GuestBookingResponse;
import com.beat.apis.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.apis.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.apis.booking.application.dto.MemberBookingRequest;
import com.beat.apis.booking.application.dto.MemberBookingResponse;
import com.beat.apis.booking.application.dto.MemberBookingRetrieveResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingFacade {
	private final MemberBookingService memberBookingService;
	private final MemberBookingRetrieveService memberBookingRetrieveService;
	private final GuestBookingService guestBookingService;
	private final GuestBookingRetrieveService guestBookingRetrieveService;
	private final BookingCancelService bookingCancelService;

	public MemberBookingResponse createMemberBooking(Long memberId, MemberBookingRequest request) {
		return memberBookingService.createMemberBooking(memberId, request);
	}

	public List<MemberBookingRetrieveResponse> findMemberBookings(Long memberId) {
		return memberBookingRetrieveService.findMemberBookings(memberId);
	}

	public GuestBookingResponse createGuestBooking(GuestBookingRequest request) {
		return guestBookingService.createGuestBooking(request);
	}

	public List<GuestBookingRetrieveResponse> findGuestBookings(GuestBookingRetrieveRequest request) {
		return guestBookingRetrieveService.findGuestBookings(request);
	}

	public BookingRefundResponse refundBooking(BookingRefundRequest request) {
		return bookingCancelService.refundBooking(request);
	}

	public BookingCancelResponse cancelBooking(BookingCancelRequest request) {
		return bookingCancelService.cancelBooking(request);
	}
}
