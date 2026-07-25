package com.beat.apis.booking.facade

data class GuestBookingSessionOutcome<T>(
    val response: T,
    val sessionToken: String?,
)
