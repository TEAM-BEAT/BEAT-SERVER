package com.beat.infrastructure.redis.auth.guest

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

// 게스트 세션 생존시간(30분): 게스트 로그인 후 예매 완료까지의 UX 상한. 쿠키 만료(__Host-guestSession)와 정책 동기 필요.
@RedisHash(value = "guestSession", timeToLive = 1800)
internal data class GuestSessionRedisHash(
    @Id val tokenHash: String,
    val userId: Long,
)
