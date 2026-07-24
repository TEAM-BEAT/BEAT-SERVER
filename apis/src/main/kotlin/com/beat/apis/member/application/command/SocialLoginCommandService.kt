package com.beat.apis.member.application.command

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.application.result.LoginSuccessResult
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.user.exception.UserApplicationErrorCode
import com.beat.contracts.auth.social.SocialLoginFailure
import com.beat.contracts.auth.social.SocialLoginPort
import com.beat.contracts.auth.social.SocialLoginRequest
import com.beat.contracts.auth.social.SocialLoginType
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class SocialLoginCommandService internal constructor(
    private val socialLoginPort: SocialLoginPort,
    private val socialLoginMemberResolver: SocialLoginMemberResolver,
    private val loginTokenIssuer: LoginTokenIssuer,
    private val userRepository: UserRepository,
) {
    fun handleSocialLogin(authorizationCode: String, command: SocialLoginCommand): LoginSuccessResult {
        val socialType = SocialType.valueOf(command.socialType.name)
        val socialMemberInfo = try {
            socialLoginPort.login(
                SocialLoginRequest(
                    authorizationCode = authorizationCode,
                    socialType = socialType.toContractType(),
                ),
            )
        } catch (failure: SocialLoginFailure) {
            throw failure.toApplicationException()
        }
        val member = socialLoginMemberResolver.findOrRegister(
            socialMemberInfo,
            SocialIdentity.of(socialType, socialMemberInfo.socialId),
        )
        val user = userRepository.findById(requireNotNull(member.userId))
            .orElseThrow { ApiApplicationException(UserApplicationErrorCode.USER_NOT_FOUND) }
        return loginTokenIssuer.issue(
            requireNotNull(member.memberId),
            user.role,
            socialMemberInfo,
        )
    }

    private fun SocialType.toContractType(): SocialLoginType = when (this) {
        SocialType.KAKAO -> SocialLoginType.KAKAO
    }

    private fun SocialLoginFailure.toApplicationException(): ApiApplicationException {
        val errorCode = when (reason) {
            SocialLoginFailure.Reason.UNSUPPORTED_SOCIAL_TYPE -> MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST
            SocialLoginFailure.Reason.AUTHENTICATION_FAILED -> MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED
            SocialLoginFailure.Reason.PROVIDER_FAILURE -> MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_FAILURE
            SocialLoginFailure.Reason.PROVIDER_UNAVAILABLE -> MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_UNAVAILABLE
            SocialLoginFailure.Reason.PROVIDER_TIMEOUT -> MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_TIMEOUT
        }
        return ApiApplicationException(
            errorCode = errorCode,
            cause = this,
        )
    }
}
