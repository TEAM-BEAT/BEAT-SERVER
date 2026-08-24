package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.BookingHistoryReadPort
import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.booking.booker.result.GuestBookingAccessOutcome
import com.beat.application.frontoffice.booking.booker.toResult
import com.beat.application.frontoffice.booking.booker.validateGuestBookingIdentity
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class GuestBookingAccessService internal constructor(
    private val credentialAuthenticator: GuestBookingCredentialAuthenticator,
    private val guestAccessThrottle: GuestAccessThrottle,
    private val bookingHistoryReadPort: BookingHistoryReadPort,
    private val guestBookingSessionManager: GuestBookingSessionManager,
    private val clock: Clock,
) {
    @Transactional
    fun authenticateAndFind(
        command: GuestBookingAuthenticationCommand,
        clientAddress: String,
    ): GuestBookingAccessOutcome = translateDomainFailure {
        val identity = validateGuestBookingIdentity(
            command.bookerName,
            command.bookerPhoneNumber,
            command.birthDate,
            command.password,
        )
        val throttleKey = listOf(clientAddress, identity.bookerName, identity.phoneNumber, identity.birthDate)
            .joinToString("|")
        if (guestAccessThrottle.isBlocked(throttleKey)) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.GUEST_ACCESS_RATE_LIMITED)
        }

        val userId = credentialAuthenticator.findUserId(
            identity.bookerName,
            identity.phoneNumber,
            identity.birthDate,
            identity.password,
        )
        if (userId == null) {
            guestAccessThrottle.recordFailure(throttleKey)
            throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
        }
        guestAccessThrottle.reset(throttleKey)

        val today = LocalDate.now(clock)
        GuestBookingAccessOutcome(
            bookings = bookingHistoryReadPort.findByUserId(userId).map { it.toResult(today) },
            sessionToken = guestBookingSessionManager.issueOrNull(userId),
        )
    }
}
