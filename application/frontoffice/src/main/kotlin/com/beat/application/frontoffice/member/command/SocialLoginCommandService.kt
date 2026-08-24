package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.auth.command.LoginSessionIssuer
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations

@Service
class SocialLoginCommandService internal constructor(
    private val socialLoginProvider: SocialLoginProvider,
    private val socialLoginMemberResolver: SocialLoginMemberResolver,
    private val loginSessionIssuer: LoginSessionIssuer,
    private val userRepository: UserRepository,
    private val transactions: TransactionOperations,
) {
    fun handleSocialLogin(authorizationCode: String, command: SocialLoginCommand): LoginSuccessResult =
        translateDomainFailure {
            val socialType = SocialType.valueOf(command.socialType.name)
            val socialLoginProfile = try {
                socialLoginProvider.login(
                    SocialLoginRequest(authorizationCode, socialType.toLoginType()),
                )
            } catch (failure: SocialLoginFailure) {
                throw failure.toApplicationException()
            }
            val socialIdentity = SocialIdentity.of(socialType, socialLoginProfile.socialId)
            val memberAndRole = try {
                requireNotNull(
                    transactions.execute {
                        val member = socialLoginMemberResolver.findOrRegister(
                            socialLoginProfile,
                            socialIdentity,
                        )
                        val user = userRepository.findById(member.userId)
                            ?: throw FrontofficeApplicationException(MemberApplicationErrorCode.USER_NOT_FOUND)
                        MemberLoginContext(member.memberId, user.role.roleName)
                    },
                ) { "Social login transaction returned no member login context" }
            } catch (duplicate: DuplicateSocialIdentityException) {
                requireNotNull(
                    transactions.execute {
                        val member = socialLoginMemberResolver.findExisting(socialIdentity)
                            ?: throw duplicate
                        val user = userRepository.findById(member.userId)
                            ?: throw FrontofficeApplicationException(MemberApplicationErrorCode.USER_NOT_FOUND)
                        MemberLoginContext(member.memberId, user.role.roleName)
                    },
                ) { "Social login duplicate recovery transaction returned no member login context" }
            }
            val loginSession = loginSessionIssuer.issueFor(
                memberId = memberAndRole.memberId,
                roleName = memberAndRole.roleName,
            )
            LoginSuccessResult(
                accessToken = loginSession.accessToken,
                refreshToken = loginSession.refreshToken,
                nickname = socialLoginProfile.nickname,
                role = memberAndRole.roleName,
            )
        }

    private data class MemberLoginContext(
        val memberId: Long,
        val roleName: String,
    )

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
