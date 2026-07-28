package com.beat.infra.redis.auth.guest

import com.beat.contracts.auth.guest.GuestAccessThrottlePort
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript

class RedisGuestAccessThrottleAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val recordGuestAccessFailureScript: RedisScript<Long>,
) : GuestAccessThrottlePort {

    override fun isBlocked(keyMaterial: String): Boolean {
        val attempts = redisTemplate.opsForValue().get(key(keyMaterial)) ?: return false
        return (attempts.toLongOrNull() ?: 0L) >= MAX_FAILURES
    }

    override fun recordFailure(keyMaterial: String) {
        redisTemplate.execute(
            recordGuestAccessFailureScript,
            listOf(key(keyMaterial)),
            WINDOW.toSeconds().toString(),
        )
    }

    override fun reset(keyMaterial: String) {
        redisTemplate.delete(key(keyMaterial))
    }

    private fun key(keyMaterial: String): String = KEY_PREFIX + Sha256Hasher.hashToBase64Url(keyMaterial)

    private companion object {
        const val MAX_FAILURES = 5L
        val WINDOW: Duration = Duration.ofMinutes(10)
        const val KEY_PREFIX = "guest-access-failure:"
    }
}
