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
import org.mockito.Mockito

class AuthenticationCommandInvariantSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("refresh token rejection mapping") {
        test("maps every security failure to its exact application error") {
            val expectedCodes = mapOf(
                TokenAuthenticationFailure.EXPIRED to TokenApplicationErrorCode.REFRESH_TOKEN_EXPIRED_ERROR,
                TokenAuthenticationFailure.INVALID_TOKEN to TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR,
                TokenAuthenticationFailure.INVALID_SIGNATURE to TokenApplicationErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR,
                TokenAuthenticationFailure.UNSUPPORTED to TokenApplicationErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR,
                TokenAuthenticationFailure.EMPTY to TokenApplicationErrorCode.REFRESH_TOKEN_EMPTY_ERROR,
            )

            expectedCodes.forEach { (failure, expectedCode) ->
                val dependencies = AuthenticationDependencies()
                Mockito.`when`(dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(TokenAuthenticationResult.Rejected(failure))

                val exception = shouldThrow<FrontofficeApplicationException> {
                    dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
                }

                exception.errorCode shouldBe expectedCode
                exception.errorCode.type shouldBe expectedCode.type
                Mockito.verifyNoInteractions(dependencies.tokenIssuer, dependencies.refreshTokenStore)
            }
        }

        test("rejects an invalid token before reading stored ownership or issuing an access token") {
            val dependencies = AuthenticationDependencies()
            Mockito.`when`(dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
                .thenReturn(TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN))

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR
            Mockito.verifyNoInteractions(dependencies.tokenIssuer, dependencies.refreshTokenStore)
        }
    }

    context("stored refresh token ownership validation") {
        test("rejects a refresh token when its stored member is missing") {
            val dependencies = AuthenticationDependencies()
            stubAuthenticatedRefreshToken(dependencies)
            Mockito.`when`(dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
                .thenReturn(null)

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.NOT_FOUND
            Mockito.verifyNoInteractions(dependencies.tokenIssuer)
        }

        test("rejects a refresh token when its stored owner differs from the token subject") {
            val dependencies = AuthenticationDependencies()
            stubAuthenticatedRefreshToken(dependencies, memberId = MEMBER_ID)
            Mockito.`when`(dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
                .thenReturn(STORED_MEMBER_ID)

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
            Mockito.verifyNoInteractions(dependencies.tokenIssuer)
        }

        test("rejects a refresh token with an invalid role claim before issuing an access token") {
            val dependencies = AuthenticationDependencies()
            stubAuthenticatedRefreshToken(dependencies, roleName = "ROLE_UNKNOWN")
            Mockito.`when`(dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
                .thenReturn(MEMBER_ID)

            val exception = shouldThrow<FrontofficeApplicationException> {
                dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            exception.errorCode shouldBe TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
            Mockito.verifyNoInteractions(dependencies.tokenIssuer)
        }
    }

    context("access token issuance") {
        test("issues an access token after authenticating and matching stored ownership") {
            val dependencies = AuthenticationDependencies()
            val subject = TokenSubject(MEMBER_ID, "ROLE_MEMBER")
            Mockito.`when`(dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
                .thenReturn(TokenAuthenticationResult.Authenticated(subject))
            Mockito.`when`(dependencies.refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN))
                .thenReturn(MEMBER_ID)
            Mockito.`when`(dependencies.tokenIssuer.issueAccessToken(subject)).thenReturn(ACCESS_TOKEN)

            val result = dependencies.service.generateAccessTokenFromRefreshToken(REFRESH_TOKEN)

            result shouldBe AccessTokenResult(ACCESS_TOKEN)
            Mockito.verify(dependencies.tokenIssuer).issueAccessToken(subject)
        }
    }

    context("sign-out idempotence") {
        test("delegates deletion when the refresh token is already absent") {
            val dependencies = AuthenticationDependencies()
            Mockito.`when`(dependencies.refreshTokenStore.delete(MEMBER_ID)).thenReturn(false)

            dependencies.service.signOut(MEMBER_ID)

            Mockito.verify(dependencies.refreshTokenStore).delete(MEMBER_ID)
        }

        test("remains idempotent across repeated deletion attempts") {
            val dependencies = AuthenticationDependencies()
            Mockito.`when`(dependencies.refreshTokenStore.delete(MEMBER_ID)).thenReturn(true, false)

            dependencies.service.signOut(MEMBER_ID)
            dependencies.service.signOut(MEMBER_ID)

            Mockito.verify(dependencies.refreshTokenStore, Mockito.times(2)).delete(MEMBER_ID)
        }
    }
})

private class AuthenticationDependencies {
    val tokenIssuer: TokenIssuer = Mockito.mock(TokenIssuer::class.java)
    val refreshTokenAuthenticator: RefreshTokenAuthenticator = Mockito.mock(RefreshTokenAuthenticator::class.java)
    val refreshTokenStore: RefreshTokenStore = Mockito.mock(RefreshTokenStore::class.java)
    val service = AuthenticationCommandService(tokenIssuer, refreshTokenAuthenticator, refreshTokenStore)
}

private fun stubAuthenticatedRefreshToken(
    dependencies: AuthenticationDependencies,
    memberId: Long = MEMBER_ID,
    roleName: String = "ROLE_MEMBER",
) {
    Mockito.`when`(dependencies.refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
        .thenReturn(TokenAuthenticationResult.Authenticated(TokenSubject(memberId, roleName)))
}

private const val REFRESH_TOKEN = "refresh-token"
private const val ACCESS_TOKEN = "access-token"
private const val MEMBER_ID = 10L
private const val STORED_MEMBER_ID = 20L
