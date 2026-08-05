package com.beat.apis.member.application.command

import com.beat.apis.member.application.result.LoginSuccessResult
import com.beat.contracts.auth.jwt.JwtSubject
import com.beat.contracts.auth.jwt.JwtTokenPort
import com.beat.contracts.auth.refreshtoken.RefreshTokenPort
import com.beat.contracts.auth.social.SocialMemberInfo
import com.beat.domain.user.model.Role
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
internal class LoginTokenIssuer(
    private val jwtTokenPort: JwtTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) {
    fun issue(
        memberId: Long,
        role: Role,
        socialMemberInfo: SocialMemberInfo,
    ): LoginSuccessResult {
        val subject = JwtSubject(
            memberId = memberId,
            roleName = role.roleName,
        )
        val refreshToken = jwtTokenPort.issueRefreshToken(subject)
        refreshTokenPort.saveRefreshToken(memberId, refreshToken)
        val accessToken = jwtTokenPort.issueAccessToken(subject)
        log.info { "Login success for role: ${role.roleName}, hasAccessToken=${accessToken.isNotBlank()}, hasRefreshToken=${refreshToken.isNotBlank()}" }
        return LoginSuccessResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            nickname = socialMemberInfo.nickname,
            role = role.roleName,
        )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
