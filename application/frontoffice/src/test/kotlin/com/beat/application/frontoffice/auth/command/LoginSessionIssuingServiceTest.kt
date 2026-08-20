package com.beat.application.frontoffice.auth.command

import com.beat.support.security.token.TokenIssuer
import com.beat.support.security.token.TokenSubject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class LoginSessionIssuingServiceTest {

    @Mock
    private lateinit var tokenIssuer: TokenIssuer

    @Mock
    private lateinit var refreshTokenStore: RefreshTokenStore

    @Test
    fun `issues refresh token saves it before issuing access token and returns the session`() {
        val subject = TokenSubject(MEMBER_ID, ROLE_NAME)
        Mockito.`when`(tokenIssuer.issueRefreshToken(subject)).thenReturn(REFRESH_TOKEN)
        Mockito.`when`(tokenIssuer.issueAccessToken(subject)).thenReturn(ACCESS_TOKEN)

        val result = LoginSessionIssuingService(tokenIssuer, refreshTokenStore).issueFor(MEMBER_ID, ROLE_NAME)

        assertEquals(LoginSession(ACCESS_TOKEN, REFRESH_TOKEN), result)
        val order = Mockito.inOrder(tokenIssuer, refreshTokenStore)
        order.verify(tokenIssuer).issueRefreshToken(subject)
        order.verify(refreshTokenStore).save(MEMBER_ID, REFRESH_TOKEN)
        order.verify(tokenIssuer).issueAccessToken(subject)
    }

    private companion object {
        const val MEMBER_ID = 10L
        const val ROLE_NAME = "ROLE_MEMBER"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
    }
}
