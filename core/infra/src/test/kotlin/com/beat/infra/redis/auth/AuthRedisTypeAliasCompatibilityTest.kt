package com.beat.infra.redis.auth

import com.beat.infra.redis.auth.guest.GuestSessionRedisHash
import com.beat.infra.redis.auth.refreshtoken.RefreshTokenRedisHash
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.convert.Bucket
import org.springframework.data.redis.core.convert.MappingRedisConverter
import org.springframework.data.redis.core.convert.RedisData
import org.springframework.data.redis.core.mapping.RedisMappingContext

class AuthRedisTypeAliasCompatibilityTest {

    private val converter = MappingRedisConverter(
        RedisMappingContext().apply {
            setInitialEntitySet(setOf(RefreshTokenRedisHash::class.java, GuestSessionRedisHash::class.java))
            afterPropertiesSet()
        },
    ).apply {
        afterPropertiesSet()
    }

    @Test
    fun `refresh token은 기존 gateway type hint를 쓰고 읽는다`() {
        val written = RedisData()
        converter.write(RefreshTokenRedisHash(1L, "refresh-token"), written)

        assertEquals(LEGACY_REFRESH_TOKEN_TYPE, written.typeHint())

        val legacyData = RedisData(
            Bucket.newBucketFromStringMap(
                mapOf(
                    "_class" to LEGACY_REFRESH_TOKEN_TYPE,
                    "id" to "1",
                    "refreshToken" to "refresh-token",
                ),
            ),
        )

        val restored = converter.read(RefreshTokenRedisHash::class.java, legacyData)

        assertEquals(1L, restored.id)
        assertEquals("refresh-token", restored.refreshToken)
    }

    @Test
    fun `guest session은 기존 gateway type hint를 쓰고 읽는다`() {
        val written = RedisData()
        converter.write(GuestSessionRedisHash("token-hash", 1L), written)

        assertEquals(LEGACY_GUEST_SESSION_TYPE, written.typeHint())

        val legacyData = RedisData(
            Bucket.newBucketFromStringMap(
                mapOf(
                    "_class" to LEGACY_GUEST_SESSION_TYPE,
                    "tokenHash" to "token-hash",
                    "userId" to "1",
                ),
            ),
        )

        val restored = converter.read(GuestSessionRedisHash::class.java, legacyData)

        assertEquals("token-hash", restored.tokenHash)
        assertEquals(1L, restored.userId)
    }

    private fun RedisData.typeHint(): String =
        String(requireNotNull(bucket.get("_class")), StandardCharsets.UTF_8)

    private companion object {
        const val LEGACY_REFRESH_TOKEN_TYPE = "com.beat.gateway.refreshtoken.internal.store.RefreshToken"
        const val LEGACY_GUEST_SESSION_TYPE = "com.beat.gateway.guest.internal.store.GuestSession"
    }
}
