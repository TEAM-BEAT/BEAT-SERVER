package com.beat.gateway.guest.internal

import com.beat.contracts.auth.guest.GuestAccessThrottlePort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Duration

class GuestAccessThrottleService(
    private val redisTemplate: StringRedisTemplate,
) : GuestAccessThrottlePort {

    override fun isBlocked(keyMaterial: String): Boolean {
        val attempts = redisTemplate.opsForValue().get(key(keyMaterial)) ?: return false
        return (attempts.toLongOrNull() ?: 0L) >= MAX_FAILURES
    }

    override fun recordFailure(keyMaterial: String) {
        redisTemplate.execute(
            RECORD_FAILURE_SCRIPT,
            listOf(key(keyMaterial)),
            WINDOW.toSeconds().toString(),
        )
    }

    override fun reset(keyMaterial: String) {
        redisTemplate.delete(key(keyMaterial))
    }

    private fun key(keyMaterial: String): String = KEY_PREFIX + Sha256Hasher.hashToBase64Url(keyMaterial)

    companion object {
        private const val MAX_FAILURES = 5L
        private val WINDOW: Duration = Duration.ofMinutes(10)
        private const val KEY_PREFIX = "guest-access-failure:"

        /** INCR과 EXPIRE를 원자적으로 수행해 window 유실을 방지한다. */
        private val RECORD_FAILURE_SCRIPT: DefaultRedisScript<Long> = DefaultRedisScript(
            "local value = redis.call('INCR', KEYS[1]); " +
                "if value == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                "return value;",
            Long::class.java,
        )
    }
}
