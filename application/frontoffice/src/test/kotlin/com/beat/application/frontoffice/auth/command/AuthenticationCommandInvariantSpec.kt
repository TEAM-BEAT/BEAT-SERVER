package com.beat.application.frontoffice.auth.command

import com.beat.application.frontoffice.auth.exception.TokenApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.support.security.token.RefreshTokenAuthenticator
import com.beat.support.security.token.TokenAuthenticationFailure
import com.beat.support.security.token.TokenAuthenticationResult
import com.beat.support.security.token.TokenIssuer
import com.beat.support.security.token.TokenSubject
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify

class AuthenticationCommandInvariantSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("refresh token 거부 매핑") {
        test("모든 보안 실패를 정확한 application 에러로 매핑한다") {
            val expectedCodes = mapOf(
                TokenAuthenticationFailure.EXPIRED to TokenApplicationErrorCode.REFRESH_TOKEN_EXPIRED_ERROR,
                TokenAuthenticationFailure.INVALID_TOKEN to TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR,
                TokenAuthenticationFailure.INVALID_SIGNATURE to TokenApplicationErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR,
                TokenAuthenticationFailure.UNSUPPORTED to TokenApplicationErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR,
                TokenAuthenticationFailure.EMPTY to TokenApplicationErrorCode.REFRESH_TOKEN_EMPTY_ERROR,
            )

            expectedCodes.forEach { (failure, expectedCode) ->
                val dependencies = AuthenticationDependencies()
                every { dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN) } returns
                    TokenAuthenticationResult.Rejected(failure)

                val exception = shouldThrow<FrontofficeApplicationException> {
                    dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
                }

                exception.errorCode shouldBe expectedCode
                exception.errorCode.type shouldBe expectedCode.type
                verify {
            listOf(dependencies.tokenIssuer, dependencies.refreshTokenStore) wasNot Called
        }
            }
        }

        test("저장된 소유자 확인이나 access token 발급 전에 유효하지 않은 token을 거부한다") {
            val dependencies = AuthenticationDependencies()
            every { dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN) } returns
                TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN)

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR
            verify {
            listOf(dependencies.tokenIssuer, dependencies.refreshTokenStore) wasNot Called
        }
        }
    }

    context("저장된 refresh token 소유자 검증") {
        test("저장된 member가 없으면 refresh token을 거부한다") {
            val dependencies = AuthenticationDependencies()
            stubAuthenticatedRefreshToken(dependencies)
            every { dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) } returns null

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.NOT_FOUND
            verify { dependencies.tokenIssuer wasNot Called }
        }

        test("저장된 소유자가 token subject와 다르면 refresh token을 거부한다") {
            val dependencies = AuthenticationDependencies()
            stubAuthenticatedRefreshToken(dependencies, memberId = MEMBER_ID)
            every { dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) } returns STORED_MEMBER_ID

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
            verify { dependencies.tokenIssuer wasNot Called }
        }

        test("access token 발급 전에 role claim이 유효하지 않은 refresh token을 거부한다") {
            val dependencies = AuthenticationDependencies()
            stubAuthenticatedRefreshToken(dependencies, roleName = "ROLE_UNKNOWN")
            every { dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) } returns MEMBER_ID

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
            verify { dependencies.tokenIssuer wasNot Called }
        }
    }

    context("access token 발급") {
        test("인증과 저장된 소유자 확인 후 access token을 발급한다") {
            val dependencies = AuthenticationDependencies()
            val subject = TokenSubject(MEMBER_ID, "ROLE_MEMBER")
            every { dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN) } returns
                TokenAuthenticationResult.Authenticated(subject)
            every { dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN) } returns MEMBER_ID
            every { dependencies.tokenIssuer.issueAccessToken(subject) } returns ACCESS_TOKEN

            val result = dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)

            result shouldBe AccessTokenResult(ACCESS_TOKEN)
            verify { dependencies.tokenIssuer.issueAccessToken(subject) }
        }
    }

    context("sign-out 멱등성") {
        test("refresh token이 이미 없어도 삭제를 위임한다") {
            val dependencies = AuthenticationDependencies()
            every { dependencies.refreshTokenStore.delete(MEMBER_ID) } returns false

            dependencies.service.signOut(MEMBER_ID)

            verify { dependencies.refreshTokenStore.delete(MEMBER_ID) }
        }

        test("반복 삭제 시도에서도 멱등을 유지한다") {
            val dependencies = AuthenticationDependencies()
            every { dependencies.refreshTokenStore.delete(MEMBER_ID) } returnsMany listOf(true, false)

            dependencies.service.signOut(MEMBER_ID)
            dependencies.service.signOut(MEMBER_ID)

            verify(exactly = 2) { dependencies.refreshTokenStore.delete(MEMBER_ID) }
        }
    }
})

private class AuthenticationDependencies {
    val tokenIssuer: TokenIssuer = mockk(relaxed = true)
    val refreshTokenAuthenticator: RefreshTokenAuthenticator = mockk(relaxed = true)
    val refreshTokenStore: RefreshTokenStore = mockk(relaxed = true)
    val service = AuthenticationCommandService(tokenIssuer, refreshTokenAuthenticator, refreshTokenStore)
}

private fun stubAuthenticatedRefreshToken(
    dependencies: AuthenticationDependencies,
    memberId: Long = MEMBER_ID,
    roleName: String = "ROLE_MEMBER",
) {
    every { dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN) } returns
        TokenAuthenticationResult.Authenticated(TokenSubject(memberId, roleName))
}

private const val REFRESH_TOKEN = "refresh-token"
private const val ACCESS_TOKEN = "access-token"
private const val MEMBER_ID = 10L
private const val STORED_MEMBER_ID = 20L
