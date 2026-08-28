package com.beat.infrastructure.redis.auth.guest

import com.beat.application.frontoffice.booking.booker.command.GuestSessionStore
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
class RedisGuestSessionAdapterIntegrationSpec : FunSpec() {

    @Autowired private lateinit var guestSessionStore: GuestSessionStore

    @Autowired private lateinit var redisTemplate: StringRedisTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        context("guest session Redis 어댑터") {
            test("발급된 session 왕복은 해시 key와 설정된 ttl을 사용한다") {
                val token = guestSessionStore.issue(USER_ID)
                val tokenHash = Sha256Hasher.hashToBase64Url(token)
                val redisKey = "$REDIS_KEY_PREFIX:$tokenHash"

                try {
                    guestSessionStore.findUserId(token) shouldBe USER_ID
                    redisTemplate.hasKey(redisKey).shouldBeTrue()
                    redisTemplate.hasKey("$REDIS_KEY_PREFIX:$token").shouldBeFalse()
                    redisTemplate.getExpire(redisKey).shouldNotBeNull().let { ttl ->
                        (ttl in 1..GUEST_SESSION_TTL_SECONDS).shouldBeTrue()
                    }
                } finally {
                    cleanupSessionKey(redisKey, tokenHash)
                }
            }
        }
    }

    private fun cleanupSessionKey(redisKey: String, tokenHash: String) {
        redisTemplate.delete(redisKey)
        redisTemplate.opsForSet().remove(REDIS_KEYSPACE_KEY, tokenHash)
    }

    private companion object {
        const val USER_ID = 101L
        const val REDIS_KEY_PREFIX = "guestSession"
        const val REDIS_KEYSPACE_KEY = "guestSession"
        const val GUEST_SESSION_TTL_SECONDS = 1_800L
    }
}
