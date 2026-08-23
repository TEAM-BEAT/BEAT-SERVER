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
import io.mockk.every
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify

class SocialLoginApplicationSpec : FunSpec({

    test("provider 실패는 정확한 member application 에러로 매핑되고 원인을 보존하며 downstream 작업을 중단한다") {
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
            val socialLoginProvider = mockk<SocialLoginProvider>(relaxed = true)
            val socialLoginMemberResolver = mockk<SocialLoginMemberResolver>(relaxed = true)
            val loginSessionIssuer = mockk<LoginSessionIssuer>(relaxed = true)
            val userRepository = mockk<UserRepository>(relaxed = true)
            every { socialLoginProvider.login(PROVIDER_REQUEST) } throws failure

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
            verify {
            listOf(socialLoginMemberResolver, loginSessionIssuer, userRepository) wasNot Called
        }
        }
    }

    test("기존 member 로그인은 연결된 user 역할로 application 세션을 구성한다") {
        val socialLoginProvider = mockk<SocialLoginProvider>(relaxed = true)
        val socialLoginMemberResolver = mockk<SocialLoginMemberResolver>(relaxed = true)
        val loginSessionIssuer = mockk<LoginSessionIssuer>(relaxed = true)
        val userRepository = mockk<UserRepository>(relaxed = true)
        val member = MemberAuthenticationResult(memberId = MEMBER_ID, userId = USER_ID)
        val user = Users.rehydrate(USER_ID, Role.MEMBER)
        val session = LoginSession(ACCESS_TOKEN, REFRESH_TOKEN)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        every { socialLoginProvider.login(PROVIDER_REQUEST) } returns PROFILE
        every { socialLoginMemberResolver.findOrRegister(PROFILE, identity) } returns member
        every { userRepository.findById(USER_ID) } returns user
        every { loginSessionIssuer.issueFor(MEMBER_ID, Role.MEMBER.roleName) } returns session

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
        verify { socialLoginProvider.login(PROVIDER_REQUEST) }
        verify { socialLoginMemberResolver.findOrRegister(PROFILE, identity) }
        verify { loginSessionIssuer.issueFor(MEMBER_ID, Role.MEMBER.roleName) }
    }

    test("연결된 user가 없으면 not found로 매핑하고 세션을 발급하지 않는다") {
        val socialLoginProvider = mockk<SocialLoginProvider>(relaxed = true)
        val socialLoginMemberResolver = mockk<SocialLoginMemberResolver>(relaxed = true)
        val loginSessionIssuer = mockk<LoginSessionIssuer>(relaxed = true)
        val userRepository = mockk<UserRepository>(relaxed = true)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        every { socialLoginProvider.login(PROVIDER_REQUEST) } returns PROFILE
        every { socialLoginMemberResolver.findOrRegister(PROFILE, identity) } returns
            MemberAuthenticationResult(MEMBER_ID, USER_ID)
        every { userRepository.findById(USER_ID) } returns null

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
        verify { loginSessionIssuer wasNot Called }
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
