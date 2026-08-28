package com.beat.infrastructure.redis.auth.guest

import com.beat.application.frontoffice.booking.booker.command.GuestAccessThrottle
import com.beat.infrastructure.redis.auth.AuthRedisConfig
import com.beat.infrastructure.support.RedisTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ContextConfiguration

@DataRedisTest
@ContextConfiguration(classes = [AuthRedisConfig::class, RedisTestContainerConfig::class])
@Tags("correctness")
class RedisGuestAccessThrottleConcurrencySpec : FunSpec() {

    @Autowired private lateinit var guestAccessThrottlePort: GuestAccessThrottle

    @Autowired private lateinit var redisTemplate: StringRedisTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("동시 20회 실패는 원자적으로 집계되어 key를 차단한다") {
            val key = redisKey(KEY_MATERIAL)
            val executor = Executors.newFixedThreadPool(WORKER_COUNT)
            val start = CountDownLatch(1)
            val done = CountDownLatch(WORKER_COUNT)
            val failures = ConcurrentLinkedQueue<Throwable>()

            try {
                redisTemplate.delete(key)
                repeat(WORKER_COUNT) {
                    executor.submit {
                        try {
                            start.await()
                            guestAccessThrottlePort.recordFailure(KEY_MATERIAL)
                        } catch (throwable: Throwable) {
                            failures.add(throwable)
                        } finally {
                            done.countDown()
                        }
                    }
                }

                start.countDown()
                done.await(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
                failures.isEmpty().shouldBeTrue()
                redisTemplate.opsForValue().get(key) shouldBe WORKER_COUNT.toString()
                guestAccessThrottlePort.isBlocked(KEY_MATERIAL).shouldBeTrue()
                redisTemplate.getExpire(key).shouldNotBeNull().let { ttl ->
                    (ttl in 1..THROTTLE_TTL_SECONDS).shouldBeTrue()
                }
            } finally {
                executor.shutdownNow()
                redisTemplate.delete(key)
            }
        }
    }

    private fun redisKey(keyMaterial: String): String =
        "$KEY_PREFIX${Sha256Hasher.hashToBase64Url(keyMaterial)}"

    private companion object {
        const val KEY_MATERIAL = "guest-access-throttle-concurrency-pr13"
        const val KEY_PREFIX = "guest-access-failure:"
        const val WORKER_COUNT = 20
        const val COMPLETION_TIMEOUT_SECONDS = 10L
        const val THROTTLE_TTL_SECONDS = 600L
    }
}
