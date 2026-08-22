package com.beat.infra.redis.auth.refreshtoken

import com.beat.application.frontoffice.auth.command.RefreshTokenStore
import com.beat.infra.redis.auth.AuthRedisConfig
import com.beat.infra.support.RedisTestContainerConfig
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
class RedisRefreshTokenAdapterIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var refreshTokenStore: RefreshTokenStore

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        context("refresh token Redis adapter") {
            test("round trip preserves the legacy type alias and configured ttl") {
                try {
                    refreshTokenStore.save(MEMBER_ID, REFRESH_TOKEN)

                    refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) shouldBe MEMBER_ID
                    redisTemplate.opsForHash<String, String>().get(REDIS_KEY, "_class") shouldBe LEGACY_TYPE
                    redisTemplate.getExpire(REDIS_KEY).shouldNotBeNull().let { ttl ->
                        (ttl in 1..REFRESH_TOKEN_TTL_SECONDS).shouldBeTrue()
                    }
                } finally {
                    cleanupRefreshTokenKeys()
                }
            }

            test("deletion removes the stored value and indexed lookup") {
                try {
                    refreshTokenStore.save(MEMBER_ID, REFRESH_TOKEN)

                    refreshTokenStore.delete(MEMBER_ID).shouldBeTrue()
                    refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) shouldBe null
                    redisTemplate.hasKey(REDIS_KEY).shouldBeFalse()
                } finally {
                    cleanupRefreshTokenKeys()
                }
            }

            test("lookup of an unstored refresh token returns null") {
                try {
                    refreshTokenStore.findMemberIdByRefreshToken(MISSING_REFRESH_TOKEN) shouldBe null
                } finally {
                    cleanupMissingRefreshTokenKeys()
                }
            }

            test("deleting an unstored member returns false without creating Redis keys or indexes") {
                try {
                    refreshTokenStore.delete(MISSING_MEMBER_ID).shouldBeFalse()
                    redisTemplate.hasKey(MISSING_REDIS_KEY).shouldBeFalse()
                    redisTemplate.hasKey(MISSING_REDIS_INDEX_KEY).shouldBeFalse()
                    redisTemplate.opsForSet()
                        .isMember(REDIS_KEYSPACE_KEY, MISSING_MEMBER_ID.toString())
                        .shouldBeFalse()
                } finally {
                    cleanupMissingRefreshTokenKeys()
                }
            }

            test("reads and deletes a legacy gateway hash") {
                try {
                    redisTemplate.opsForHash<String, String>().putAll(
                        REDIS_KEY,
                        mapOf(
                            "_class" to LEGACY_TYPE,
                            "id" to MEMBER_ID.toString(),
                            "refreshToken" to REFRESH_TOKEN,
                        ),
                    )
                    redisTemplate.opsForSet().add(REDIS_KEYSPACE_KEY, MEMBER_ID.toString())
                    redisTemplate.opsForSet().add(REDIS_INDEX_KEY, MEMBER_ID.toString())
                    redisTemplate.opsForSet().add(REDIS_INDEX_METADATA_KEY, REDIS_INDEX_KEY)

                    refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) shouldBe MEMBER_ID
                    refreshTokenStore.delete(MEMBER_ID).shouldBeTrue()
                    redisTemplate.hasKey(REDIS_KEY).shouldBeFalse()
                } finally {
                    cleanupRefreshTokenKeys()
                }
            }
        }
    }

    private fun cleanupRefreshTokenKeys() {
        redisTemplate.opsForSet().remove(REDIS_KEYSPACE_KEY, MEMBER_ID.toString())
        redisTemplate.opsForSet().remove(REDIS_INDEX_KEY, MEMBER_ID.toString())
        redisTemplate.delete(REDIS_KEY)
        redisTemplate.delete(REDIS_INDEX_KEY)
        redisTemplate.delete(REDIS_INDEX_METADATA_KEY)
    }

    private fun cleanupMissingRefreshTokenKeys() {
        redisTemplate.opsForSet().remove(REDIS_KEYSPACE_KEY, MISSING_MEMBER_ID.toString())
        redisTemplate.delete(MISSING_REDIS_KEY)
        redisTemplate.delete(MISSING_REDIS_INDEX_KEY)
    }

    private companion object {
        const val MEMBER_ID = 1L
        const val REFRESH_TOKEN = "refresh-token-pr13"
        const val MISSING_MEMBER_ID = 404L
        const val MISSING_REFRESH_TOKEN = "missing-refresh-token-pr13"
        const val LEGACY_TYPE = "com.beat.gateway.refreshtoken.internal.store.RefreshToken"
        const val REDIS_KEYSPACE_KEY = "refreshToken"
        const val REDIS_KEY = "refreshToken:$MEMBER_ID"
        const val REDIS_INDEX_KEY = "refreshToken:refreshToken:$REFRESH_TOKEN"
        const val REDIS_INDEX_METADATA_KEY = "$REDIS_KEY:idx"
        const val MISSING_REDIS_KEY = "refreshToken:$MISSING_MEMBER_ID"
        const val MISSING_REDIS_INDEX_KEY = "refreshToken:refreshToken:$MISSING_REFRESH_TOKEN"
        const val REFRESH_TOKEN_TTL_SECONDS = 1_209_600L
    }
}
