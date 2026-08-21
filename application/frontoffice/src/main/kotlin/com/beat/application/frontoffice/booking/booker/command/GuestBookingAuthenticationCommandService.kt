package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.booking.booker.validateGuestBookingIdentity
import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.contracts.auth.guest.GuestAccessThrottlePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GuestBookingAuthenticationCommandService internal constructor(
    private val credentialAuthenticator: GuestBookingCredentialAuthenticator,
    private val guestAccessThrottlePort: GuestAccessThrottlePort,
) {
    @Transactional
    fun authenticate(command: GuestBookingAuthenticationCommand, clientAddress: String): Long {
        val identity = validateGuestBookingIdentity(
            command.bookerName,
            command.bookerPhoneNumber,
            command.birthDate,
            command.password,
        )
        val throttleKey = listOf(clientAddress, identity.bookerName, identity.phoneNumber, identity.birthDate)
            .joinToString("|")
        if (guestAccessThrottlePort.isBlocked(throttleKey)) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.GUEST_ACCESS_RATE_LIMITED)
        }

        val userId = credentialAuthenticator.findUserId(
            identity.bookerName,
            identity.phoneNumber,
            identity.birthDate,
            identity.password,
        )
        if (userId == null) {
            guestAccessThrottlePort.recordFailure(throttleKey)
            throw FrontofficeApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
        }
        guestAccessThrottlePort.reset(throttleKey)
        return userId
    }

}
