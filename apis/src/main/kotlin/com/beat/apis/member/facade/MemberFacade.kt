package com.beat.apis.member.facade

import com.beat.apis.member.application.command.AuthenticationCommandService
import com.beat.apis.member.application.command.SocialLoginCommandService
import com.beat.apis.member.application.command.SocialLoginCommand
import com.beat.apis.member.application.command.SocialLoginProvider
import com.beat.apis.member.api.request.MemberLoginRequest
import com.beat.apis.member.api.response.AccessTokenGenerateResponse
import com.beat.apis.member.api.response.MemberLoginResponse
import org.springframework.stereotype.Service

@Service
class MemberFacade(
    private val authenticationCommandService: AuthenticationCommandService,
    private val socialLoginCommandService: SocialLoginCommandService,
) {
    fun handleSocialLogin(authorizationCode: String, request: MemberLoginRequest): MemberLoginSession =
        socialLoginCommandService.handleSocialLogin(
            authorizationCode,
            SocialLoginCommand(
                socialType = SocialLoginProvider.valueOf(requireNotNull(request.socialType).name),
            ),
        ).let { result ->
            MemberLoginSession(
                response = MemberLoginResponse.from(result),
                refreshToken = requireNotNull(result.refreshToken),
            )
        }

    fun generateAccessTokenFromRefreshToken(refreshToken: String): AccessTokenGenerateResponse =
        AccessTokenGenerateResponse.from(authenticationCommandService.generateAccessTokenFromRefreshToken(refreshToken))

    fun signOut(memberId: Long) = authenticationCommandService.signOut(memberId)
}

data class MemberLoginSession(
    val response: MemberLoginResponse,
    val refreshToken: String,
)
