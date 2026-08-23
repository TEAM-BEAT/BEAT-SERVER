package com.beat.apis.member.facade

import com.beat.apis.member.api.request.MemberLoginRequest
import com.beat.apis.member.api.type.SocialTypeRequest
import com.beat.apis.member.api.response.AccessTokenGenerateResponse
import com.beat.apis.member.api.response.MemberLoginResponse
import com.beat.application.frontoffice.auth.command.AuthenticationCommandService
import com.beat.application.frontoffice.member.command.SocialLoginCommand
import com.beat.application.frontoffice.member.command.SocialLoginCommandService
import com.beat.application.frontoffice.member.command.SocialLoginType
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
                socialType = request.socialType.toApplicationType(),
            ),
        ).let { result ->
            MemberLoginSession(
                response = MemberLoginResponse.from(result),
                refreshToken = result.refreshToken,
            )
        }

    fun generateAccessTokenFromRefreshToken(refreshToken: String): AccessTokenGenerateResponse =
        AccessTokenGenerateResponse.from(authenticationCommandService.generateAccessTokenFromRefreshToken(refreshToken))

    fun signOut(memberId: Long) = authenticationCommandService.signOut(memberId)

    private fun SocialTypeRequest.toApplicationType(): SocialLoginType = when (this) {
        SocialTypeRequest.KAKAO -> SocialLoginType.KAKAO
    }
}

data class MemberLoginSession(
    val response: MemberLoginResponse,
    val refreshToken: String,
)
