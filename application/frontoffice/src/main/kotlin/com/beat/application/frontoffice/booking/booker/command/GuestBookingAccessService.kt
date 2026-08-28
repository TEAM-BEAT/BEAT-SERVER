package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.command.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.booking.booker.command.result.GuestBookingAccessResult
import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GuestBookingAccessService
internal constructor(
    private val credentialAuthenticator: GuestBookingCredentialAuthenticator,
    private val guestAccessThrottle: GuestAccessThrottle,
    private val guestBookingSessionManager: GuestBookingSessionManager,
) {
    @Transactional
    fun authenticate(
        command: GuestBookingAuthenticationCommand,
        clientAddress: String,
    ): GuestBookingAccessResult = translateDomainFailure {
        val identity =
            validateGuestBookingIdentity(
                command.bookerName,
                command.bookerPhoneNumber,
                command.birthDate,
                command.password,
            )
        val throttleKey =
            listOf(clientAddress, identity.bookerName, identity.phoneNumber, identity.birthDate)
                .joinToString("|")
        if (guestAccessThrottle.isBlocked(throttleKey)) {
            throw FrontofficeApplicationException(
                BookingApplicationErrorCode.GUEST_ACCESS_RATE_LIMITED
            )
        }

        val userId =
            credentialAuthenticator.findUserId(
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

        GuestBookingAccessResult(userId = userId)
    }

    fun issueSession(userId: Long): String? = guestBookingSessionManager.issueOrNull(userId)
}
