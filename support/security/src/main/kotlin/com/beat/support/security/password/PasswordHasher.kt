package com.beat.support.security.password

interface PasswordHasher {

    fun encode(rawPassword: String): String

    fun matches(rawPassword: String, storedPassword: String): Boolean

    fun needsUpgrade(storedPassword: String): Boolean
}
