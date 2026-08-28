package com.beat.apps.api.booking.facade

import com.beat.application.frontoffice.booking.booker.command.GuestBookingAccessService
import com.beat.application.frontoffice.booking.booker.command.result.GuestBookingAccessResult
import com.beat.application.frontoffice.booking.booker.query.GuestBookingHistoryQueryService
import com.beat.apps.api.booking.api.request.GuestBookingRetrieveRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class BookingFacadeSpec :
    FunSpec({
        test("guest query 조회가 실패하면 session 발급을 시도하지 않는다") {
            val accessService = mockk<GuestBookingAccessService>()
            val historyQueryService = mockk<GuestBookingHistoryQueryService>()
            val facade = facade(accessService, historyQueryService)
            every { accessService.authenticate(any(), CLIENT_ADDRESS) } returns
                GuestBookingAccessResult(USER_ID)
            every { historyQueryService.findGuestBookings(USER_ID) } throws
                IllegalStateException("query unavailable")

            shouldThrow<IllegalStateException> {
                facade.findGuestBookings(retrieveRequest(), CLIENT_ADDRESS)
            }

            verify(exactly = 0) { accessService.issueSession(any()) }
        }
    })

private fun facade(
    accessService: GuestBookingAccessService,
    historyQueryService: GuestBookingHistoryQueryService,
): BookingFacade =
    BookingFacade(
        memberBookingCommandService = mockk(relaxed = true),
        memberBookingQueryService = mockk(relaxed = true),
        guestBookingCommandService = mockk(relaxed = true),
        guestBookingAccessService = accessService,
        guestBookingHistoryQueryService = historyQueryService,
        bookingCancellationCommandService = mockk(relaxed = true),
    )

private fun retrieveRequest(): GuestBookingRetrieveRequest =
    GuestBookingRetrieveRequest(
        bookerName = "booker",
        birthDate = "990101",
        bookerPhoneNumber = "010-0000-0000",
        password = "1234",
    )

private const val USER_ID = 7L
private const val CLIENT_ADDRESS = "127.0.0.1"
