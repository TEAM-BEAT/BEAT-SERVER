package com.beat.infra.auth.redis.refreshtoken

import java.util.Optional
import org.springframework.data.repository.CrudRepository

interface RefreshTokenRedisRepository : CrudRepository<RefreshTokenRedisHash, Long> {

    fun findByRefreshToken(refreshToken: String): Optional<RefreshTokenRedisHash>
}
