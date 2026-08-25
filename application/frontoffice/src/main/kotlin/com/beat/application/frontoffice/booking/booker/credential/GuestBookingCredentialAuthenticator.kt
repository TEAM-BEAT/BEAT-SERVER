package com.beat.application.frontoffice.booking.booker.credential

import com.beat.application.frontoffice.security.PasswordHasher
import org.springframework.stereotype.Component

@Component
internal class GuestBookingCredentialAuthenticator(
    private val passwordHasher: PasswordHasher,
    private val guestBookingCredentialRepository: GuestBookingCredentialRepository,
) {
    fun findUserId(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
        rawPassword: String,
    ): Long? {
        val matchedCredentials =
            guestBookingCredentialRepository
                .findCandidates(bookerName, phoneNumber, birthDate)
                .filter { credential ->
                    passwordHasher.matches(rawPassword, credential.encodedPassword)
                }
        val matchingUserIds = matchedCredentials.map { it.userId }.distinct()
        if (matchingUserIds.size != 1) {
            return null
        }

        val userId = matchingUserIds.single()
        if (matchedCredentials.any { passwordHasher.needsUpgrade(it.encodedPassword) }) {
            guestBookingCredentialRepository.replaceEncodedPassword(
                userId,
                passwordHasher.encode(rawPassword),
            )
        }
        return userId
    }

    fun encode(rawPassword: String): String = passwordHasher.encode(rawPassword)
}
