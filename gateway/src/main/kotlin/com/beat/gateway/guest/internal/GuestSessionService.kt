package com.beat.gateway.guest.internal

import com.beat.contracts.auth.guest.GuestSessionPort
import com.beat.gateway.guest.internal.store.GuestSession
import com.beat.gateway.guest.internal.store.GuestSessionRepository
import java.security.SecureRandom
import java.util.Base64
import java.util.OptionalLong

class GuestSessionService(
    private val guestSessionRepository: GuestSessionRepository,
) : GuestSessionPort {

    private val secureRandom = SecureRandom()

    override fun issue(userId: Long): String {
        val token = generateToken()
        guestSessionRepository.save(GuestSession(Sha256Hasher.hashToBase64Url(token), userId))
        return token
    }

    override fun findUserId(token: String): OptionalLong {
        if (token.isBlank()) {
            return OptionalLong.empty()
        }
        return guestSessionRepository.findById(Sha256Hasher.hashToBase64Url(token))
            .map { OptionalLong.of(it.userId) }
            .orElseGet { OptionalLong.empty() }
    }

    private fun generateToken(): String {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
    }

    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
    }
}
