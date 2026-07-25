package com.beat.contracts.auth.guest

interface GuestPasswordHashPort {

    fun encode(rawPassword: String): String

    fun matches(rawPassword: String, storedPassword: String): Boolean

    fun needsUpgrade(storedPassword: String): Boolean
}
