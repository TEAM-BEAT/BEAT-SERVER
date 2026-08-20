package com.beat.application.frontoffice.auth.command

import com.beat.application.frontoffice.auth.exception.TokenApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.support.security.token.RefreshTokenAuthenticator
import com.beat.support.security.token.TokenAuthenticationFailure
import com.beat.support.security.token.TokenAuthenticationResult
import com.beat.support.security.token.TokenIssuer
import com.beat.support.security.token.TokenSubject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AuthenticationCommandServiceTest {

    @Mock
    private lateinit var tokenIssuer: TokenIssuer

    @Mock
    private lateinit var refreshTokenAuthenticator: RefreshTokenAuthenticator

    @Mock
    private lateinit var refreshTokenStore: RefreshTokenStore

    @Test
    fun `maps every refresh token authentication failure to its exact application error`() {
        val expectedCodes = mapOf(
            TokenAuthenticationFailure.EXPIRED to TokenApplicationErrorCode.REFRESH_TOKEN_EXPIRED_ERROR,
            TokenAuthenticationFailure.INVALID_TOKEN to TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR,
            TokenAuthenticationFailure.INVALID_SIGNATURE to TokenApplicationErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR,
            TokenAuthenticationFailure.UNSUPPORTED to TokenApplicationErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR,
            TokenAuthenticationFailure.EMPTY to TokenApplicationErrorCode.REFRESH_TOKEN_EMPTY_ERROR,
        )

        expectedCodes.forEach { (failure, expectedCode) ->
            Mockito.`when`(refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
                .thenReturn(TokenAuthenticationResult.Rejected(failure))

            val exception = assertThrows<FrontofficeApplicationException> {
                service().generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
            }

            assertEquals(expectedCode, exception.errorCode)
            assertEquals(expectedCode.type, exception.errorCode.type)
        }
        Mockito.verifyNoInteractions(tokenIssuer, refreshTokenStore)
    }

    @Test
    fun `rejects a refresh token when its stored member is missing`() {
        stubAuthenticatedRefreshToken()
        Mockito.`when`(refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN)).thenReturn(null)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
        }

        assertEquals(TokenApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.errorCode)
        assertEquals(FrontofficeApplicationErrorType.NOT_FOUND, exception.errorCode.type)
        Mockito.verifyNoInteractions(tokenIssuer)
    }

    @Test
    fun `rejects a refresh token when its stored owner differs from the token subject`() {
        stubAuthenticatedRefreshToken(memberId = 10L)
        Mockito.`when`(refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN)).thenReturn(20L)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
        }

        assertEquals(TokenApplicationErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR, exception.errorCode)
        assertEquals(FrontofficeApplicationErrorType.INVALID_INPUT, exception.errorCode.type)
        Mockito.verifyNoInteractions(tokenIssuer)
    }

    @Test
    fun `rejects a refresh token with an invalid role claim`() {
        stubAuthenticatedRefreshToken(roleName = "ROLE_UNKNOWN")
        Mockito.`when`(refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN)).thenReturn(10L)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
        }

        assertEquals(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.errorCode)
        assertEquals(FrontofficeApplicationErrorType.INVALID_INPUT, exception.errorCode.type)
        Mockito.verifyNoInteractions(tokenIssuer)
    }

    @Test
    fun `issues an access token after validating and matching the stored owner`() {
        val subject = TokenSubject(10L, "ROLE_MEMBER")
        Mockito.`when`(refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
            .thenReturn(TokenAuthenticationResult.Authenticated(subject))
        Mockito.`when`(refreshTokenStore.findMemberIdByRefreshToken(REFRESH_TOKEN)).thenReturn(10L)
        Mockito.`when`(tokenIssuer.issueAccessToken(subject)).thenReturn(ACCESS_TOKEN)

        val result = service().generateAccessTokenFromRefreshToken(REFRESH_TOKEN)

        assertEquals(AccessTokenResult(ACCESS_TOKEN), result)
        Mockito.verify(tokenIssuer).issueAccessToken(subject)
    }

    @Test
    fun `rejects an invalid token before extracting claims or reading the store`() {
        Mockito.`when`(refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
            .thenReturn(TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_TOKEN))

        val exception = assertThrows<FrontofficeApplicationException> {
            service().generateAccessTokenFromRefreshToken(REFRESH_TOKEN)
        }

        assertEquals(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.errorCode)
        Mockito.verifyNoInteractions(tokenIssuer, refreshTokenStore)
    }

    @Test
    fun `signout delegates deletion even when the refresh token is already absent`() {
        Mockito.`when`(refreshTokenStore.delete(MEMBER_ID)).thenReturn(false)

        service().signOut(MEMBER_ID)

        Mockito.verify(refreshTokenStore).delete(MEMBER_ID)
    }

    @Test
    fun `signout remains idempotent across repeated deletion attempts`() {
        Mockito.`when`(refreshTokenStore.delete(MEMBER_ID)).thenReturn(true, false)

        service().signOut(MEMBER_ID)
        service().signOut(MEMBER_ID)

        Mockito.verify(refreshTokenStore, Mockito.times(2)).delete(MEMBER_ID)
    }

    private fun stubAuthenticatedRefreshToken(
        memberId: Long = 10L,
        roleName: String = "ROLE_MEMBER",
    ) {
        Mockito.`when`(refreshTokenAuthenticator.authenticateRefreshToken(REFRESH_TOKEN))
            .thenReturn(TokenAuthenticationResult.Authenticated(TokenSubject(memberId, roleName)))
    }

    private fun service() = AuthenticationCommandService(
        tokenIssuer = tokenIssuer,
        refreshTokenAuthenticator = refreshTokenAuthenticator,
        refreshTokenStore = refreshTokenStore,
    )

    private companion object {
        const val REFRESH_TOKEN = "refresh-token"
        const val ACCESS_TOKEN = "access-token"
        const val MEMBER_ID = 10L
    }
}
