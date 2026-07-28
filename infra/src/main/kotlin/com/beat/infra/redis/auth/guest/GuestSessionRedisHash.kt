package com.beat.infra.redis.auth.guest

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.redis.core.RedisHash

@TypeAlias("com.beat.gateway.guest.internal.store.GuestSession")
@RedisHash(value = "guestSession", timeToLive = 1800)
class GuestSessionRedisHash(
    @Id
    val tokenHash: String,
    val userId: Long,
)
