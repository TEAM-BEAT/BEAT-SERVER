package com.beat.application.frontoffice.booking.credential

import com.beat.support.security.password.PasswordHasher
import com.beat.domain.booking.repository.BookingRepository
import org.springframework.stereotype.Component

@Component
internal class GuestBookingCredentialAuthenticator(
    private val bookingRepository: BookingRepository,
    private val passwordHasher: PasswordHasher,
    private val guestBookingCredentialRepository: GuestBookingCredentialRepository,
) {
    fun findUserId(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
        rawPassword: String,
    ): Long? {
        val matchedCredential = guestBookingCredentialRepository.findCandidates(bookerName, phoneNumber, birthDate)
            .firstOrNull { credential ->
                passwordHasher.matches(rawPassword, credential.encodedPassword)
            }
            ?: return null
        upgradePasswordIfNeeded(matchedCredential, rawPassword)
        return matchedCredential.userId
    }

    fun encode(rawPassword: String): String = passwordHasher.encode(rawPassword)

    private fun upgradePasswordIfNeeded(credential: GuestBookingCredential, rawPassword: String) {
        if (passwordHasher.needsUpgrade(credential.encodedPassword)) {
            bookingRepository.replaceGuestPassword(credential.userId, passwordHasher.encode(rawPassword))
        }
    }
}
