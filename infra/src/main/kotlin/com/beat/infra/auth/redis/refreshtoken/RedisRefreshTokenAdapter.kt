package com.beat.infra.auth.redis.refreshtoken

import com.beat.contracts.auth.RefreshTokenPort
import java.util.OptionalLong

class RedisRefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRedisRepository,
) : RefreshTokenPort {

    override fun saveRefreshToken(memberId: Long, refreshToken: String) {
        refreshTokenRepository.save(RefreshTokenRedisHash(memberId, refreshToken))
    }

    override fun findMemberIdByRefreshToken(refreshToken: String): OptionalLong =
        refreshTokenRepository.findByRefreshToken(refreshToken)
            .map { token -> OptionalLong.of(token.id) }
            .orElseGet { OptionalLong.empty() }

    override fun deleteRefreshToken(memberId: Long): Boolean =
        refreshTokenRepository.findById(memberId).map { token ->
            refreshTokenRepository.delete(token)
            true
        }.orElse(false)
}
