package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.auth.command.LoginSession
import com.beat.application.frontoffice.auth.command.LoginSessionIssuer
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

class SocialLoginApplicationSpec : FunSpec({

    test("provider failures map to the exact member application error, preserve cause, and stop downstream work") {
        val failures = listOf(
            SocialLoginFailure.unsupportedSocialType() to MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST,
            SocialLoginFailure.authenticationFailed() to MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED,
            SocialLoginFailure.providerFailure(RuntimeException("malformed")) to
                MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_FAILURE,
            SocialLoginFailure.providerUnavailable(RuntimeException("unavailable")) to
                MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_UNAVAILABLE,
            SocialLoginFailure.providerTimeout(RuntimeException("timeout")) to
                MemberApplicationErrorCode.SOCIAL_LOGIN_PROVIDER_TIMEOUT,
        )

        failures.forEach { (failure, expectedCode) ->
            val socialLoginProvider = Mockito.mock(SocialLoginProvider::class.java)
            val socialLoginMemberResolver = Mockito.mock(SocialLoginMemberResolver::class.java)
            val loginSessionIssuer = Mockito.mock(LoginSessionIssuer::class.java)
            val userRepository = Mockito.mock(UserRepository::class.java)
            Mockito.`when`(socialLoginProvider.login(PROVIDER_REQUEST)).thenThrow(failure)

            val exception = shouldThrow<FrontofficeApplicationException> {
                service(
                    socialLoginProvider,
                    socialLoginMemberResolver,
                    loginSessionIssuer,
                    userRepository,
                ).handleSocialLogin(AUTHORIZATION_CODE, SocialLoginCommand(SocialLoginType.KAKAO))
            }

            exception.errorCode shouldBe expectedCode
            exception.errorCode.type shouldBe expectedCode.type
            exception.cause shouldBe failure
            Mockito.verifyNoInteractions(socialLoginMemberResolver, loginSessionIssuer, userRepository)
        }
    }

    test("existing member login composes the application session with the linked user role") {
        val socialLoginProvider = Mockito.mock(SocialLoginProvider::class.java)
        val socialLoginMemberResolver = Mockito.mock(SocialLoginMemberResolver::class.java)
        val loginSessionIssuer = Mockito.mock(LoginSessionIssuer::class.java)
        val userRepository = Mockito.mock(UserRepository::class.java)
        val member = MemberAuthenticationResult(memberId = MEMBER_ID, userId = USER_ID)
        val user = Users.rehydrate(USER_ID, Role.MEMBER)
        val session = LoginSession(ACCESS_TOKEN, REFRESH_TOKEN)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        Mockito.`when`(socialLoginProvider.login(PROVIDER_REQUEST)).thenReturn(PROFILE)
        Mockito.`when`(socialLoginMemberResolver.findOrRegister(PROFILE, identity)).thenReturn(member)
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(user)
        Mockito.`when`(loginSessionIssuer.issueFor(MEMBER_ID, Role.MEMBER.roleName)).thenReturn(session)

        val result = service(
            socialLoginProvider,
            socialLoginMemberResolver,
            loginSessionIssuer,
            userRepository,
        ).handleSocialLogin(AUTHORIZATION_CODE, SocialLoginCommand(SocialLoginType.KAKAO))

        result shouldBe LoginSuccessResult(
            accessToken = ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
            nickname = PROFILE.nickname,
            role = Role.MEMBER.roleName,
        )
        Mockito.verify(socialLoginProvider).login(PROVIDER_REQUEST)
        Mockito.verify(socialLoginMemberResolver).findOrRegister(PROFILE, identity)
        Mockito.verify(loginSessionIssuer).issueFor(MEMBER_ID, Role.MEMBER.roleName)
    }

    test("missing linked user maps to not found and does not issue a session") {
        val socialLoginProvider = Mockito.mock(SocialLoginProvider::class.java)
        val socialLoginMemberResolver = Mockito.mock(SocialLoginMemberResolver::class.java)
        val loginSessionIssuer = Mockito.mock(LoginSessionIssuer::class.java)
        val userRepository = Mockito.mock(UserRepository::class.java)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        Mockito.`when`(socialLoginProvider.login(PROVIDER_REQUEST)).thenReturn(PROFILE)
        Mockito.`when`(socialLoginMemberResolver.findOrRegister(PROFILE, identity))
            .thenReturn(MemberAuthenticationResult(MEMBER_ID, USER_ID))
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(null)

        val exception = shouldThrow<FrontofficeApplicationException> {
            service(
                socialLoginProvider,
                socialLoginMemberResolver,
                loginSessionIssuer,
                userRepository,
            ).handleSocialLogin(AUTHORIZATION_CODE, SocialLoginCommand(SocialLoginType.KAKAO))
        }

        exception.errorCode shouldBe MemberApplicationErrorCode.USER_NOT_FOUND
        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.NOT_FOUND
        Mockito.verifyNoInteractions(loginSessionIssuer)
    }
})

private fun service(
    socialLoginProvider: SocialLoginProvider,
    socialLoginMemberResolver: SocialLoginMemberResolver,
    loginSessionIssuer: LoginSessionIssuer,
    userRepository: UserRepository,
): SocialLoginCommandService = SocialLoginCommandService(
    socialLoginProvider = socialLoginProvider,
    socialLoginMemberResolver = socialLoginMemberResolver,
    loginSessionIssuer = loginSessionIssuer,
    userRepository = userRepository,
)

private const val AUTHORIZATION_CODE = "authorization-code"
private const val MEMBER_ID = 1L
private const val USER_ID = 2L
private const val ACCESS_TOKEN = "access-token"
private const val REFRESH_TOKEN = "refresh-token"
private val PROFILE = SocialLoginProfile(123L, "nickname", "email@test.com")
private val PROVIDER_REQUEST = SocialLoginRequest(AUTHORIZATION_CODE, SocialLoginType.KAKAO)
