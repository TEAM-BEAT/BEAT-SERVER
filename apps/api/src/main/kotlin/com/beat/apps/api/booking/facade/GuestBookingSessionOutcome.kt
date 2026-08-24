package com.beat.apps.api.booking.facade

data class GuestBookingSessionOutcome<T>(
    val response: T,
    val sessionToken: String?,
)
