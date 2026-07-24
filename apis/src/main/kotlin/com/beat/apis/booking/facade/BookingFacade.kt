package com.beat.apis.booking.facade

import com.beat.apis.booking.application.command.BookingCancellationCommandService
import com.beat.apis.booking.application.command.BookingCancelCommand
import com.beat.apis.booking.application.command.BookingRefundCommand
import com.beat.apis.booking.application.command.GuestBookingCommandService
import com.beat.apis.booking.application.command.GuestBookingCommand
import com.beat.apis.booking.application.command.GuestBookingAuthenticationCommand
import com.beat.apis.booking.application.command.GuestBookingAuthenticationCommandService
import com.beat.apis.booking.application.command.GuestBookingSessionCommandService
import com.beat.apis.booking.application.command.MemberBookingCommandService
import com.beat.apis.booking.application.command.MemberBookingCommand
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
import com.beat.apis.booking.application.query.GuestBookingQueryService
import com.beat.apis.booking.application.query.MemberBookingQueryService
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
                    scheduleId = requireNotNull(request.scheduleId),
                    purchaseTicketCount = requireNotNull(request.purchaseTicketCount),
                    bookerName = requireNotNull(request.bookerName),
                    bookerPhoneNumber = requireNotNull(request.bookerPhoneNumber),
                ),
            ),
        )

    fun findMemberBookings(memberId: Long): List<MemberBookingRetrieveResponse> =
        memberBookingQueryService.findMemberBookings(memberId).map(MemberBookingRetrieveResponse::from)

    fun createGuestBooking(request: GuestBookingRequest): GuestBookingSessionOutcome<GuestBookingResponse> {
        val response = GuestBookingResponse.from(
            guestBookingCommandService.createGuestBooking(
                GuestBookingCommand.of(
                    scheduleId = requireNotNull(request.scheduleId),
                    purchaseTicketCount = requireNotNull(request.purchaseTicketCount),
                    bookerName = requireNotNull(request.bookerName),
                    bookerPhoneNumber = requireNotNull(request.bookerPhoneNumber),
                    birthDate = requireNotNull(request.birthDate),
                    password = requireNotNull(request.password),
                ),
            ),
        )
        return GuestBookingSessionOutcome(
            response = response,
            sessionToken = issueGuestSessionOrNull(requireNotNull(response.userId)),
        )
    }

    fun findGuestBookings(
        request: GuestBookingRetrieveRequest,
        clientAddress: String,
    ): GuestBookingSessionOutcome<List<GuestBookingRetrieveResponse>> {
        val userId = guestBookingAuthenticationCommandService.authenticate(
            GuestBookingAuthenticationCommand.of(
                bookerName = requireNotNull(request.bookerName),
                birthDate = requireNotNull(request.birthDate),
                bookerPhoneNumber = requireNotNull(request.bookerPhoneNumber),
                password = requireNotNull(request.password),
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
                bookingId = requireNotNull(request.bookingId),
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
            BookingCancelCommand.from(requireNotNull(request.bookingId)),
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
