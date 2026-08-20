package com.beat.application.frontoffice.booking.credential

import com.beat.domain.booking.repository.BookingRepository
import com.beat.support.security.password.PasswordHasher
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
        val matchedCredentials = guestBookingCredentialRepository.findCandidates(bookerName, phoneNumber, birthDate)
            .filter { credential ->
                passwordHasher.matches(rawPassword, credential.encodedPassword)
            }
        val matchingUserIds = matchedCredentials.map { it.userId }.distinct()
        if (matchingUserIds.size != 1) {
            return null
        }

        val userId = matchingUserIds.single()
        if (matchedCredentials.any { passwordHasher.needsUpgrade(it.encodedPassword) }) {
            bookingRepository.replaceGuestPassword(userId, passwordHasher.encode(rawPassword))
        }
        return userId
    }

    fun encode(rawPassword: String): String = passwordHasher.encode(rawPassword)
}
