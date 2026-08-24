package com.beat.apps.api.booking.api

import com.beat.apps.api.guest.GUEST_SESSION_COOKIE_NAME
import com.beat.apps.api.booking.api.request.BookingCancelRequest
import com.beat.apps.api.booking.api.request.BookingRefundRequest
import com.beat.apps.api.booking.api.request.GuestBookingRequest
import com.beat.apps.api.booking.api.request.GuestBookingRetrieveRequest
import com.beat.apps.api.booking.api.request.MemberBookingRequest
import com.beat.apps.api.booking.api.response.BookingCancelResponse
import com.beat.apps.api.booking.api.response.BookingRefundResponse
import com.beat.apps.api.booking.api.response.BookingSuccessCode
import com.beat.apps.api.booking.api.response.GuestBookingResponse
import com.beat.apps.api.booking.api.response.GuestBookingRetrieveResponse
import com.beat.apps.api.booking.api.response.MemberBookingResponse
import com.beat.apps.api.booking.api.response.MemberBookingRetrieveResponse
import com.beat.apps.api.booking.facade.BookingFacade
import com.beat.support.security.CurrentMember
import com.beat.apps.api.response.SuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingFacade: BookingFacade,
) : BookingApi {

    @PostMapping("/member")
    override fun createMemberBooking(
        @CurrentMember memberId: Long,
        @Valid @RequestBody memberBookingRequest: MemberBookingRequest,
    ): ResponseEntity<SuccessResponse<MemberBookingResponse>> {
        val response = bookingFacade.createMemberBooking(memberId, memberBookingRequest)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SuccessResponse.of(BookingSuccessCode.MEMBER_BOOKING_SUCCESS, response))
    }

    @GetMapping("/member/retrieve")
    override fun getMemberBookings(
        @CurrentMember memberId: Long,
    ): ResponseEntity<SuccessResponse<List<MemberBookingRetrieveResponse>>> {
        val response = bookingFacade.findMemberBookings(memberId)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(BookingSuccessCode.MEMBER_BOOKING_RETRIEVE_SUCCESS, response))
    }

    @PostMapping("/guest")
    override fun createGuestBookings(
        @Valid @RequestBody guestBookingRequest: GuestBookingRequest,
        httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<GuestBookingResponse>> {
        val result = bookingFacade.createGuestBooking(guestBookingRequest)
        setGuestSessionCookieIfPresent(httpServletResponse, result.sessionToken)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SuccessResponse.of(BookingSuccessCode.GUEST_BOOKING_SUCCESS, result.response))
    }

    @PostMapping("/guest/retrieve")
    override fun getGuestBookings(
        @Valid @RequestBody guestBookingRetrieveRequest: GuestBookingRetrieveRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<List<GuestBookingRetrieveResponse>>> {
        val result = bookingFacade.findGuestBookings(guestBookingRetrieveRequest, httpServletRequest.remoteAddr)
        setGuestSessionCookieIfPresent(httpServletResponse, result.sessionToken)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(BookingSuccessCode.GUEST_BOOKING_RETRIEVE_SUCCESS, result.response))
    }

    @PatchMapping("/refund")
    override fun refundBookings(
        @CurrentMember memberId: Long?,
        @CookieValue(value = GUEST_SESSION_COOKIE_NAME, required = false) guestSessionToken: String?,
        @Valid @RequestBody bookingRefundRequest: BookingRefundRequest,
    ): ResponseEntity<SuccessResponse<BookingRefundResponse>> {
        val response = bookingFacade.refundBooking(memberId, guestSessionToken, bookingRefundRequest)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(BookingSuccessCode.BOOKING_REFUND_SUCCESS, response))
    }

    @PatchMapping("/cancel")
    override fun cancelBookings(
        @CurrentMember memberId: Long?,
        @CookieValue(value = GUEST_SESSION_COOKIE_NAME, required = false) guestSessionToken: String?,
        @Valid @RequestBody bookingCancelRequest: BookingCancelRequest,
    ): ResponseEntity<SuccessResponse<BookingCancelResponse>> {
        val response = bookingFacade.cancelBooking(memberId, guestSessionToken, bookingCancelRequest)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(BookingSuccessCode.BOOKING_CANCEL_SUCCESS, response))
    }

    private fun setGuestSessionCookieIfPresent(response: HttpServletResponse, sessionToken: String?) {
        if (sessionToken == null) {
            return
        }
        val cookie = ResponseCookie.from(GUEST_SESSION_COOKIE_NAME, sessionToken)
            .maxAge(GUEST_SESSION_MAX_AGE.toLong())
            .path("/")
            .secure(true)
            .sameSite("Strict")
            .httpOnly(true)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private companion object {
        const val GUEST_SESSION_MAX_AGE = 30 * 60
    }
}
