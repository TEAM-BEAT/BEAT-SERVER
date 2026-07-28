package com.beat.gateway.refreshtoken.internal.store

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@RedisHash(value = "refreshToken", timeToLive = 1209600)
class RefreshToken(
    @Id
    val id: Long,
    @Indexed
    val refreshToken: String,
)
