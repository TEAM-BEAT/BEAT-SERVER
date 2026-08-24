package com.beat.application.frontoffice.booking.booker.command

interface GuestAccessThrottle {
    fun isBlocked(keyMaterial: String): Boolean

    fun recordFailure(keyMaterial: String)

    fun reset(keyMaterial: String)
}
