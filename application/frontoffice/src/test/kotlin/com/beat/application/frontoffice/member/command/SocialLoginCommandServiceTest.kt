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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SocialLoginCommandServiceTest {

    @Mock
    private lateinit var socialLoginProvider: SocialLoginProvider

    @Mock
    private lateinit var socialLoginMemberResolver: SocialLoginMemberResolver

    @Mock
    private lateinit var loginSessionIssuer: LoginSessionIssuer

    @Mock
    private lateinit var userRepository: UserRepository

    @Test
    fun `maps all provider failures to their exact member application errors`() {
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
            Mockito.reset(socialLoginProvider)
            Mockito.`when`(socialLoginProvider.login(PROVIDER_REQUEST)).thenAnswer { throw failure }

            val exception = assertThrows<FrontofficeApplicationException> {
                service().handleSocialLogin(AUTHORIZATION_CODE, SocialLoginCommand(SocialLoginType.KAKAO))
            }

            assertEquals(expectedCode, exception.errorCode)
            assertEquals(expectedCode.type, exception.errorCode.type)
            assertSame(failure, exception.cause)
        }
        Mockito.verifyNoInteractions(socialLoginMemberResolver, loginSessionIssuer, userRepository)
    }

    @Test
    fun `composes login result for an existing member through the application-owned issuer`() {
        val member = MemberAuthenticationResult(memberId = MEMBER_ID, userId = USER_ID)
        val user = Users.rehydrate(USER_ID, Role.MEMBER)
        val session = LoginSession(ACCESS_TOKEN, REFRESH_TOKEN)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        Mockito.`when`(socialLoginProvider.login(PROVIDER_REQUEST)).thenReturn(PROFILE)
        Mockito.`when`(socialLoginMemberResolver.findOrRegister(PROFILE, identity)).thenReturn(member)
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))
        Mockito.`when`(loginSessionIssuer.issueFor(MEMBER_ID, Role.MEMBER.roleName)).thenReturn(session)

        val result = service().handleSocialLogin(
            AUTHORIZATION_CODE,
            SocialLoginCommand(SocialLoginType.KAKAO),
        )

        assertEquals(
            LoginSuccessResult(
                accessToken = ACCESS_TOKEN,
                refreshToken = REFRESH_TOKEN,
                nickname = PROFILE.nickname,
                role = Role.MEMBER.roleName,
            ),
            result,
        )
        Mockito.verify(socialLoginProvider).login(PROVIDER_REQUEST)
        Mockito.verify(socialLoginMemberResolver).findOrRegister(PROFILE, identity)
        Mockito.verify(loginSessionIssuer).issueFor(MEMBER_ID, Role.MEMBER.roleName)
    }

    @Test
    fun `rejects a resolved member whose linked user no longer exists`() {
        Mockito.`when`(socialLoginProvider.login(PROVIDER_REQUEST)).thenReturn(PROFILE)
        Mockito.`when`(
            socialLoginMemberResolver.findOrRegister(
                PROFILE,
                SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId),
            ),
        )
            .thenReturn(MemberAuthenticationResult(MEMBER_ID, USER_ID))
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(Optional.empty())

        val exception = assertThrows<FrontofficeApplicationException> {
            service().handleSocialLogin(AUTHORIZATION_CODE, SocialLoginCommand(SocialLoginType.KAKAO))
        }

        assertEquals(MemberApplicationErrorCode.USER_NOT_FOUND, exception.errorCode)
        assertEquals(FrontofficeApplicationErrorType.NOT_FOUND, exception.errorCode.type)
        Mockito.verifyNoInteractions(loginSessionIssuer)
    }

    private fun service() = SocialLoginCommandService(
        socialLoginProvider = socialLoginProvider,
        socialLoginMemberResolver = socialLoginMemberResolver,
        loginSessionIssuer = loginSessionIssuer,
        userRepository = userRepository,
    )

    private companion object {
        const val AUTHORIZATION_CODE = "authorization-code"
        const val MEMBER_ID = 1L
        const val USER_ID = 2L
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        val PROFILE = SocialLoginProfile(123L, "nickname", "email@test.com")
        val PROVIDER_REQUEST = SocialLoginRequest(AUTHORIZATION_CODE, SocialLoginType.KAKAO)
    }
}
