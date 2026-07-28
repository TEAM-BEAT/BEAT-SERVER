package com.beat.gateway.refreshtoken.internal.store

import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface RefreshTokenRepository : CrudRepository<RefreshToken, Long> {

    fun findByRefreshToken(refreshToken: String): Optional<RefreshToken>
}
