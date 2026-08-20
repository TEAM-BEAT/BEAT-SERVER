package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.auth.command.LoginSessionIssuer
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class SocialLoginCommandService internal constructor(
    private val socialLoginProvider: SocialLoginProvider,
    private val socialLoginMemberResolver: SocialLoginMemberResolver,
    private val loginSessionIssuer: LoginSessionIssuer,
    private val userRepository: UserRepository,
) {
    fun handleSocialLogin(authorizationCode: String, command: SocialLoginCommand): LoginSuccessResult {
        val socialType = SocialType.valueOf(command.socialType.name)
        val socialLoginProfile = try {
            socialLoginProvider.login(
                SocialLoginRequest(authorizationCode, socialType.toLoginType()),
            )
        } catch (failure: SocialLoginFailure) {
            throw failure.toApplicationException()
        }
        val member = socialLoginMemberResolver.findOrRegister(
            socialLoginProfile,
            SocialIdentity.of(socialType, socialLoginProfile.socialId),
        )
        val user = userRepository.findById(member.userId)
            .orElseThrow { FrontofficeApplicationException(MemberApplicationErrorCode.USER_NOT_FOUND) }
        val loginSession = loginSessionIssuer.issueFor(
            memberId = member.memberId,
            roleName = user.role.roleName,
        )
        return LoginSuccessResult(
            accessToken = loginSession.accessToken,
            refreshToken = loginSession.refreshToken,
            nickname = socialLoginProfile.nickname,
            role = user.role.roleName,
        )
    }

    private fun SocialType.toLoginType(): SocialLoginType = when (this) {
        SocialType.KAKAO -> SocialLoginType.KAKAO
    }

    private fun SocialLoginFailure.toApplicationException(): FrontofficeApplicationException {
        val errorCode = when (reason) {
            SocialLoginFailure.Reason.UNSUPPORTED_SOCIAL_TYPE -> MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST
            SocialLoginFailure.Reason.AUTHENTICATION_FAILED -> MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED
            SocialLoginFailure.Reason.PROVIDER_FAILURE -> MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_FAILURE
            SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE -> MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_UNAVAILABLE
            SocialLoginFailure.Reason.PROVIDER_TIMEOUT -> MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_TIMEOUT
        }
        return FrontofficeApplicationException(
            errorCode = errorCode,
            cause = this,
        )
    }
}
