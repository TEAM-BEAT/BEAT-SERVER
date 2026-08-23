package com.beat.apis.booking.facade

import com.beat.application.frontoffice.booking.booker.command.BookingCancellationCommandService
import com.beat.application.frontoffice.booking.booker.command.BookingCancelCommand
import com.beat.application.frontoffice.booking.booker.command.BookingRefundCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommandService
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingAuthenticationCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingAuthenticationCommandService
import com.beat.application.frontoffice.booking.booker.command.GuestBookingSessionCommandService
import com.beat.application.frontoffice.booking.booker.command.MemberBookingCommandService
import com.beat.application.frontoffice.booking.booker.command.MemberBookingCommand
import com.beat.apis.booking.api.request.BookingCancelRequest
import com.beat.apis.booking.api.response.BookingCancelResponse
import com.beat.apis.booking.api.request.BookingRefundRequest
import com.beat.apis.booking.api.response.BookingRefundResponse
import com.beat.apis.booking.api.request.GuestBookingRequest
import com.beat.apis.booking.api.response.GuestBookingResponse
import com.beat.apis.booking.api.request.GuestBookingRetrieveRequest
import com.beat.apis.booking.api.response.GuestBookingRetrieveResponse
import com.beat.apis.booking.api.request.MemberBookingRequest
import com.beat.apis.booking.api.response.MemberBookingResponse
import com.beat.apis.booking.api.response.MemberBookingRetrieveResponse
import com.beat.application.frontoffice.booking.booker.query.GuestBookingQueryService
import com.beat.application.frontoffice.booking.booker.query.MemberBookingQueryService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class BookingFacade(
    private val memberBookingCommandService: MemberBookingCommandService,
    private val memberBookingQueryService: MemberBookingQueryService,
    private val guestBookingCommandService: GuestBookingCommandService,
    private val guestBookingQueryService: GuestBookingQueryService,
    private val guestBookingAuthenticationCommandService: GuestBookingAuthenticationCommandService,
    private val guestBookingSessionCommandService: GuestBookingSessionCommandService,
    private val bookingCancellationCommandService: BookingCancellationCommandService,
) {
    fun createMemberBooking(memberId: Long, request: MemberBookingRequest): MemberBookingResponse =
        MemberBookingResponse.from(
            memberBookingCommandService.createMemberBooking(
                memberId,
                MemberBookingCommand.of(
                    scheduleId = request.scheduleId,
                    purchaseTicketCount = request.purchaseTicketCount,
                    bookerName = request.bookerName,
                    bookerPhoneNumber = request.bookerPhoneNumber,
                ),
            ),
        )

    fun findMemberBookings(memberId: Long): List<MemberBookingRetrieveResponse> =
        memberBookingQueryService.findMemberBookings(memberId).map(MemberBookingRetrieveResponse::from)

    fun createGuestBooking(request: GuestBookingRequest): GuestBookingSessionOutcome<GuestBookingResponse> {
        val response = GuestBookingResponse.from(
            guestBookingCommandService.createGuestBooking(
                GuestBookingCommand.of(
                    scheduleId = request.scheduleId,
                    purchaseTicketCount = request.purchaseTicketCount,
                    bookerName = request.bookerName,
                    bookerPhoneNumber = request.bookerPhoneNumber,
                    birthDate = request.birthDate,
                    password = request.password,
                ),
            ),
        )
        return GuestBookingSessionOutcome(
            response = response,
            sessionToken = issueGuestSessionOrNull(response.userId),
        )
    }

    fun findGuestBookings(
        request: GuestBookingRetrieveRequest,
        clientAddress: String,
    ): GuestBookingSessionOutcome<List<GuestBookingRetrieveResponse>> {
        val userId = guestBookingAuthenticationCommandService.authenticate(
            GuestBookingAuthenticationCommand.of(
                bookerName = request.bookerName,
                birthDate = request.birthDate,
                bookerPhoneNumber = request.bookerPhoneNumber,
                password = request.password,
            ),
            clientAddress,
        )
        return GuestBookingSessionOutcome(
            response = guestBookingQueryService.findGuestBookings(userId).map(GuestBookingRetrieveResponse::from),
            sessionToken = issueGuestSessionOrNull(userId),
        )
    }

    fun refundBooking(
        memberId: Long?,
        guestSessionToken: String?,
        request: BookingRefundRequest,
    ): BookingRefundResponse = BookingRefundResponse.from(
        bookingCancellationCommandService.refundBooking(
            guestBookingSessionCommandService.resolveActorUserId(memberId, guestSessionToken),
            BookingRefundCommand.of(
                bookingId = request.bookingId,
                bankName = request.bankName?.name,
                accountNumber = request.accountNumber,
                accountHolder = request.accountHolder,
            ),
        ),
    )

    fun cancelBooking(
        memberId: Long?,
        guestSessionToken: String?,
        request: BookingCancelRequest,
    ): BookingCancelResponse = BookingCancelResponse.from(
        bookingCancellationCommandService.cancelBooking(
            guestBookingSessionCommandService.resolveActorUserId(memberId, guestSessionToken),
            BookingCancelCommand.from(request.bookingId),
        ),
    )

    private fun issueGuestSessionOrNull(userId: Long): String? = try {
        guestBookingSessionCommandService.issue(userId)
    } catch (exception: RuntimeException) {
        log.error(exception) { "Guest session issuance failed after successful booking flow: userId=${userId}" }
        null
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
