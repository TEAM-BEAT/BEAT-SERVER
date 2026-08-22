package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.booking.booker.validateGuestBookingIdentity
import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GuestBookingAuthenticationCommandService internal constructor(
    private val credentialAuthenticator: GuestBookingCredentialAuthenticator,
    private val guestAccessThrottle: GuestAccessThrottle,
) {
    @Transactional
    fun authenticate(command: GuestBookingAuthenticationCommand, clientAddress: String): Long {
        return translateDomainFailure {
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
            userId
        }
    }

}
