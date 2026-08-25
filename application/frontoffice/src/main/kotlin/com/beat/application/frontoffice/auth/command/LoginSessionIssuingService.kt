package com.beat.application.frontoffice.auth.command

import com.beat.application.frontoffice.security.TokenIssuer
import com.beat.application.frontoffice.security.TokenSubject
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
internal class LoginSessionIssuingService(
    private val tokenIssuer: TokenIssuer,
    private val refreshTokenStore: RefreshTokenStore,
) : LoginSessionIssuer {
    override fun issueFor(memberId: Long, roleName: String): LoginSession {
        val subject = TokenSubject(memberId, roleName)
        val refreshToken = tokenIssuer.issueRefreshToken(subject)
        refreshTokenStore.save(subject.memberId, refreshToken)
        val accessToken = tokenIssuer.issueAccessToken(subject)
        log.info {
            "Login success for role: ${subject.roleName}, " +
                "hasAccessToken=${accessToken.isNotBlank()}, hasRefreshToken=${refreshToken.isNotBlank()}"
        }
        return LoginSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
