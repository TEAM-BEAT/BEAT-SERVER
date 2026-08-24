package com.beat.support.security.password.internal

import com.beat.application.frontoffice.security.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class BCryptPasswordHasher : PasswordHasher {

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): String = passwordEncoder.encode(rawPassword)!!

    override fun matches(rawPassword: String, storedPassword: String): Boolean {
        if (storedPassword.isBlank()) {
            return false
        }
        if (isBcrypt(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword)
        }
        return MessageDigest.isEqual(
            rawPassword.toByteArray(StandardCharsets.UTF_8),
            storedPassword.toByteArray(StandardCharsets.UTF_8),
        )
    }

    override fun needsUpgrade(storedPassword: String): Boolean =
        !isBcrypt(storedPassword) || passwordEncoder.upgradeEncoding(storedPassword)

    private fun isBcrypt(storedPassword: String): Boolean = storedPassword.startsWith(BCRYPT_PREFIX)

    companion object {
        private const val BCRYPT_PREFIX = "\$2"
    }
}
