package com.beat.contracts.auth.guest

interface GuestAccessThrottlePort {

    fun isBlocked(keyMaterial: String): Boolean

    fun recordFailure(keyMaterial: String)

    fun reset(keyMaterial: String)
}
