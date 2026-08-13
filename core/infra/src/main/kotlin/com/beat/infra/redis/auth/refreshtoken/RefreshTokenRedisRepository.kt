package com.beat.infra.redis.auth.refreshtoken

import java.util.Optional
import org.springframework.data.repository.CrudRepository

interface RefreshTokenRedisRepository : CrudRepository<RefreshTokenRedisHash, Long> {

    fun findByRefreshToken(refreshToken: String): Optional<RefreshTokenRedisHash>
}
