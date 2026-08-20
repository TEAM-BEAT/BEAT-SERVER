package com.beat.infra.redis.auth.refreshtoken

import com.beat.application.frontoffice.auth.command.RefreshTokenStore
import com.beat.infra.redis.auth.AuthRedisConfig
import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [RedisRefreshTokenAdapterIntegrationTest.TestApplication::class])
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
class RedisRefreshTokenAdapterIntegrationTest {

    @Autowired
    private lateinit var refreshTokenStore: RefreshTokenStore

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @AfterEach
    fun tearDown() {
        refreshTokenStore.delete(MEMBER_ID)
        redisTemplate.delete(REDIS_KEY)
        redisTemplate.delete(REDIS_KEYSPACE_KEY)
        redisTemplate.delete(REDIS_INDEX_KEY)
        redisTemplate.delete(REDIS_INDEX_METADATA_KEY)
    }

    @Test
    fun `refresh token round trip preserves serialization and configured ttl`() {
        refreshTokenStore.save(MEMBER_ID, REFRESH_TOKEN)

        assertEquals(MEMBER_ID, refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
        assertEquals(LEGACY_TYPE, redisTemplate.opsForHash<String, String>().get(REDIS_KEY, "_class"))

        val ttl = requireNotNull(redisTemplate.getExpire(REDIS_KEY))
        assertTrue(ttl in 1..REFRESH_TOKEN_TTL_SECONDS)
    }

    @Test
    fun `refresh token deletion removes the indexed value`() {
        refreshTokenStore.save(MEMBER_ID, REFRESH_TOKEN)

        assertTrue(refreshTokenStore.delete(MEMBER_ID))
        assertNull(refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
        assertFalse(redisTemplate.hasKey(REDIS_KEY))
    }

    @Test
    fun `legacy gateway hash can be read and deleted by infra adapter`() {
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

        assertEquals(MEMBER_ID, refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
        assertTrue(refreshTokenStore.delete(MEMBER_ID))
        assertFalse(redisTemplate.hasKey(REDIS_KEY))
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(DataRedisAutoConfiguration::class)
    @Import(AuthRedisConfig::class)
    class TestApplication

    private companion object {
        const val MEMBER_ID = 1L
        const val REFRESH_TOKEN = "refresh-token"
        const val LEGACY_TYPE = "com.beat.gateway.refreshtoken.internal.store.RefreshToken"
        const val REDIS_KEYSPACE_KEY = "refreshToken"
        const val REDIS_KEY = "refreshToken:$MEMBER_ID"
        const val REDIS_INDEX_KEY = "refreshToken:refreshToken:$REFRESH_TOKEN"
        const val REDIS_INDEX_METADATA_KEY = "$REDIS_KEY:idx"
        const val REFRESH_TOKEN_TTL_SECONDS = 1_209_600L

        @Container
        @JvmStatic
        val redis = RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))

        @DynamicPropertySource
        @JvmStatic
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port", redis::getFirstMappedPort)
        }
    }
}
