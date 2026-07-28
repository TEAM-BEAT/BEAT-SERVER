package com.beat.gateway.refreshtoken.internal

import com.beat.contracts.auth.RefreshTokenPort
import com.beat.gateway.refreshtoken.internal.store.RefreshToken
import com.beat.gateway.refreshtoken.internal.store.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.OptionalLong

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
) : RefreshTokenPort {

    private val log = LoggerFactory.getLogger(RefreshTokenService::class.java)

    override fun saveRefreshToken(memberId: Long, refreshToken: String) {
        refreshTokenRepository.save(RefreshToken(memberId, refreshToken))
    }

    override fun findMemberIdByRefreshToken(refreshToken: String): OptionalLong =
        refreshTokenRepository.findByRefreshToken(refreshToken)
            .map { token -> OptionalLong.of(token.id) }
            .orElseGet { OptionalLong.empty() }

    override fun deleteRefreshToken(memberId: Long): Boolean =
        refreshTokenRepository.findById(memberId).map { token ->
            refreshTokenRepository.delete(token)
            log.info("Deleted refresh token for memberId={}", memberId)
            true
        }.orElse(false)
}
