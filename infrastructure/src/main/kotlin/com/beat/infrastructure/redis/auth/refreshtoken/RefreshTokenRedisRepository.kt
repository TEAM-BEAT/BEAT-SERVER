package com.beat.infrastructure.redis.auth.refreshtoken

import org.springframework.data.repository.CrudRepository

internal interface RefreshTokenRedisRepository : CrudRepository<RefreshTokenRedisHash, Long> {

    fun findByRefreshToken(refreshToken: String): RefreshTokenRedisHash?
}
