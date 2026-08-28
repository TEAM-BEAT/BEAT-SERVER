package com.beat.apps.api.booking.facade

import com.beat.application.frontoffice.booking.booker.command.BookingActorCommand
import com.beat.application.frontoffice.booking.booker.command.BookingCancelCommand
import com.beat.application.frontoffice.booking.booker.command.BookingCancellationCommandService
import com.beat.application.frontoffice.booking.booker.command.BookingRefundCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingAccessService
import com.beat.application.frontoffice.booking.booker.command.GuestBookingAuthenticationCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommandService
import com.beat.application.frontoffice.booking.booker.command.MemberBookingCommand
import com.beat.application.frontoffice.booking.booker.command.MemberBookingCommandService
import com.beat.application.frontoffice.booking.booker.query.MemberBookingQueryService
import com.beat.apps.api.booking.api.request.BookingCancelRequest
import com.beat.apps.api.booking.api.request.BookingRefundRequest
import com.beat.apps.api.booking.api.request.GuestBookingRequest
import com.beat.apps.api.booking.api.request.GuestBookingRetrieveRequest
import com.beat.apps.api.booking.api.request.MemberBookingRequest
import com.beat.apps.api.booking.api.response.BookingCancelResponse
import com.beat.apps.api.booking.api.response.BookingRefundResponse
import com.beat.apps.api.booking.api.response.GuestBookingResponse
import com.beat.apps.api.booking.api.response.GuestBookingRetrieveResponse
import com.beat.apps.api.booking.api.response.MemberBookingResponse
import com.beat.apps.api.booking.api.response.MemberBookingRetrieveResponse
import org.springframework.stereotype.Service

@Service
class BookingFacade(
    private val memberBookingCommandService: MemberBookingCommandService,
    private val memberBookingQueryService: MemberBookingQueryService,
    private val guestBookingCommandService: GuestBookingCommandService,
    private val guestBookingAccessService: GuestBookingAccessService,
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
            )
        )

    fun findMemberBookings(memberId: Long): List<MemberBookingRetrieveResponse> =
        memberBookingQueryService
            .findMemberBookings(memberId)
            .map(MemberBookingRetrieveResponse::from)

    fun createGuestBooking(
        request: GuestBookingRequest
    ): GuestBookingSessionOutcome<GuestBookingResponse> {
        val outcome =
            guestBookingCommandService.createGuestBooking(
                GuestBookingCommand.of(
                    scheduleId = request.scheduleId,
                    purchaseTicketCount = request.purchaseTicketCount,
                    bookerName = request.bookerName,
                    bookerPhoneNumber = request.bookerPhoneNumber,
                    birthDate = request.birthDate,
                    password = request.password,
                )
            )
        return GuestBookingSessionOutcome(
            response = GuestBookingResponse.from(outcome.booking),
            sessionToken = outcome.sessionToken,
        )
    }

    fun findGuestBookings(
        request: GuestBookingRetrieveRequest,
        clientAddress: String,
    ): GuestBookingSessionOutcome<List<GuestBookingRetrieveResponse>> {
        val outcome =
            guestBookingAccessService.authenticateAndFind(
                GuestBookingAuthenticationCommand.of(
                    bookerName = request.bookerName,
                    birthDate = request.birthDate,
                    bookerPhoneNumber = request.bookerPhoneNumber,
                    password = request.password,
                ),
                clientAddress,
            )
        return GuestBookingSessionOutcome(
            response = outcome.bookings.map(GuestBookingRetrieveResponse::from),
            sessionToken = outcome.sessionToken,
        )
    }

    fun refundBooking(
        memberId: Long?,
        guestSessionToken: String?,
        request: BookingRefundRequest,
    ): BookingRefundResponse =
        BookingRefundResponse.from(
            bookingCancellationCommandService.refundBooking(
                BookingActorCommand(memberId, guestSessionToken),
                BookingRefundCommand.of(
                    bookingId = request.bookingId,
                    bankName = request.bankName.name,
                    accountNumber = request.accountNumber,
                    accountHolder = request.accountHolder,
                ),
            )
        )

    fun cancelBooking(
        memberId: Long?,
        guestSessionToken: String?,
        request: BookingCancelRequest,
    ): BookingCancelResponse =
        BookingCancelResponse.from(
            bookingCancellationCommandService.cancelBooking(
                BookingActorCommand(memberId, guestSessionToken),
                BookingCancelCommand.from(request.bookingId),
            )
        )
}
