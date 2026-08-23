package com.beat.infra.redis.auth.guest

import com.beat.application.frontoffice.booking.booker.command.GuestSessionStore
import java.security.SecureRandom
import java.util.Base64
import org.springframework.data.repository.findByIdOrNull

internal class RedisGuestSessionAdapter(
    private val guestSessionRepository: GuestSessionRedisRepository,
) : GuestSessionStore {

    private val secureRandom = SecureRandom()

    override fun issue(userId: Long): String {
        val token = generateToken()
        guestSessionRepository.save(GuestSessionRedisHash(Sha256Hasher.hashToBase64Url(token), userId))
        return token
    }

    override fun findUserId(token: String): Long? {
        if (token.isBlank()) {
            return null
        }
        return guestSessionRepository.findByIdOrNull(Sha256Hasher.hashToBase64Url(token))?.userId
    }

    private fun generateToken(): String {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
    }

    private companion object {
        const val TOKEN_BYTE_LENGTH = 32
    }
}
