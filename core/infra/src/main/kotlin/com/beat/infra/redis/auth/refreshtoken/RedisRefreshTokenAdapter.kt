package com.beat.infra.redis.auth.refreshtoken

import com.beat.application.frontoffice.auth.command.RefreshTokenStore

internal class RedisRefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRedisRepository,
) : RefreshTokenStore {

    override fun save(memberId: Long, refreshToken: String) {
        refreshTokenRepository.save(RefreshTokenRedisHash(memberId, refreshToken))
    }

    override fun findMemberIdByRefreshToken(refreshToken: String): Long? =
        refreshTokenRepository.findByRefreshToken(refreshToken)?.id

    override fun delete(memberId: Long): Boolean {
        val token = refreshTokenRepository.findById(memberId).orElse(null) ?: return false
        refreshTokenRepository.delete(token)
        return true
    }
}
