package com.beat.support.security.password.internal

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BCryptPasswordHasherTest {

    private val passwordHasher = BCryptPasswordHasher()

    @Test
    fun `bcrypt password matches only the original password`() {
        val encoded = passwordHasher.encode("secret")

        assertTrue(passwordHasher.matches("secret", encoded))
        assertFalse(passwordHasher.matches("wrong", encoded))
        assertFalse(passwordHasher.needsUpgrade(encoded))
    }

    @Test
    fun `blank stored password is rejected`() {
        assertFalse(passwordHasher.matches("secret", ""))
        assertFalse(passwordHasher.matches("secret", "   "))
    }

    @Test
    fun `legacy plaintext password is supported and marked for upgrade`() {
        assertTrue(passwordHasher.matches("legacy", "legacy"))
        assertFalse(passwordHasher.matches("wrong", "legacy"))
        assertTrue(passwordHasher.needsUpgrade("legacy"))
    }

    @Test
    fun `encoding produces a new bcrypt password`() {
        val encoded = passwordHasher.encode("secret")

        assertTrue(encoded.startsWith("\$2"))
        assertNotEquals("secret", encoded)
    }
}
