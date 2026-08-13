package com.beat.infra.redis.auth.guest

import com.beat.contracts.auth.guest.GuestSessionPort
import java.security.SecureRandom
import java.util.Base64
import java.util.OptionalLong

class RedisGuestSessionAdapter(
    private val guestSessionRepository: GuestSessionRedisRepository,
) : GuestSessionPort {

    private val secureRandom = SecureRandom()

    override fun issue(userId: Long): String {
        val token = generateToken()
        guestSessionRepository.save(GuestSessionRedisHash(Sha256Hasher.hashToBase64Url(token), userId))
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

    private companion object {
        const val TOKEN_BYTE_LENGTH = 32
    }
}
