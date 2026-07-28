package com.beat.infra.auth.redis.refreshtoken

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@TypeAlias("com.beat.gateway.refreshtoken.internal.store.RefreshToken")
@RedisHash(value = "refreshToken", timeToLive = 1209600)
class RefreshTokenRedisHash(
    @Id
    val id: Long,
    @Indexed
    val refreshToken: String,
)
