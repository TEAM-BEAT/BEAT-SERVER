package com.beat.apis.booking.application.credential

import com.beat.contracts.auth.guest.GuestPasswordHashPort
import com.beat.contracts.auth.guest.GuestCredentialReadPort
import com.beat.contracts.auth.guest.readmodel.GuestCredentialReadModel
import com.beat.domain.booking.repository.BookingRepository
import org.springframework.stereotype.Component

@Component
internal class GuestBookingCredentialAuthenticator(
    private val bookingRepository: BookingRepository,
    private val guestPasswordHashPort: GuestPasswordHashPort,
    private val guestCredentialReadPort: GuestCredentialReadPort,
) {
    fun findUserId(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
        rawPassword: String,
    ): Long? {
        val matchedCredential = guestCredentialReadPort.findCandidates(bookerName, phoneNumber, birthDate)
            .firstOrNull { credential ->
                guestPasswordHashPort.matches(rawPassword, credential.encodedPassword)
            }
            ?: return null
        upgradePasswordIfNeeded(matchedCredential, rawPassword)
        return matchedCredential.userId
    }

    fun encode(rawPassword: String): String = guestPasswordHashPort.encode(rawPassword)

    private fun upgradePasswordIfNeeded(credential: GuestCredentialReadModel, rawPassword: String) {
        if (guestPasswordHashPort.needsUpgrade(credential.encodedPassword)) {
            bookingRepository.replaceGuestPassword(credential.userId, guestPasswordHashPort.encode(rawPassword))
        }
    }
}
