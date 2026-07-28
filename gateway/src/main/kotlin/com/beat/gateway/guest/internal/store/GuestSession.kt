package com.beat.gateway.guest.internal.store

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

@RedisHash(value = "guestSession", timeToLive = 1800)
class GuestSession(
    @Id
    val tokenHash: String,
    val userId: Long,
)
