package com.beat.application.frontoffice.booking.booker.credential

/**
 * Reads guest credentials from the authoritative Booking persistence source.
 * Implementations must not use a cache, replica, or eventually consistent projection.
 */
fun interface GuestBookingCredentialRepository {
    fun findCandidates(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
    ): List<GuestBookingCredential>
}

data class GuestBookingCredential(
    val userId: Long,
    val encodedPassword: String,
)
