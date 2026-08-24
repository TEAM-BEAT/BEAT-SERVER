package com.beat.infrastructure.redis.auth.guest

import com.beat.application.frontoffice.booking.booker.command.GuestAccessThrottle
import com.beat.infrastructure.redis.auth.AuthRedisConfig
import com.beat.infrastructure.support.RedisTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ContextConfiguration

@DataRedisTest
@ContextConfiguration(classes = [AuthRedisConfig::class, RedisTestContainerConfig::class])
@Tags("integration")
class RedisGuestAccessThrottleIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var guestAccessThrottlePort: GuestAccessThrottle

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("guest 접근 실패는 5회 시도에서 차단되고 reset으로 차단이 해제된다") {
            val key = redisKey(KEY_MATERIAL)
            try {
                guestAccessThrottlePort.isBlocked(KEY_MATERIAL).shouldBeFalse()

                repeat(MAX_FAILURES) {
                    guestAccessThrottlePort.recordFailure(KEY_MATERIAL)
                }

                redisTemplate.opsForValue().get(key) shouldBe MAX_FAILURES.toString()
                guestAccessThrottlePort.isBlocked(KEY_MATERIAL).shouldBeTrue()
                redisTemplate.getExpire(key).shouldNotBeNull().let { ttl ->
                    (ttl in 1..THROTTLE_TTL_SECONDS).shouldBeTrue()
                }

                guestAccessThrottlePort.reset(KEY_MATERIAL)

                guestAccessThrottlePort.isBlocked(KEY_MATERIAL).shouldBeFalse()
                redisTemplate.hasKey(key).shouldBeFalse()
            } finally {
                redisTemplate.delete(key)
            }
        }
    }

    private fun redisKey(keyMaterial: String): String =
        "$KEY_PREFIX${Sha256Hasher.hashToBase64Url(keyMaterial)}"

    private companion object {
        const val KEY_MATERIAL = "guest-access-throttle-pr13"
        const val KEY_PREFIX = "guest-access-failure:"
        const val MAX_FAILURES = 5
        const val THROTTLE_TTL_SECONDS = 600L
    }
}
