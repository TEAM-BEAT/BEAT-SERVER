package com.beat.application.frontoffice.auth.command

import com.beat.support.security.token.TokenIssuer
import com.beat.support.security.token.TokenSubject
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

class LoginSessionIssuingOrderSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("access token 발급 전에 refresh token을 저장하고 세션을 반환한다") {
        val tokenIssuer = Mockito.mock(TokenIssuer::class.java)
        val refreshTokenStore = Mockito.mock(RefreshTokenStore::class.java)
        val subject = TokenSubject(MEMBER_ID, ROLE_NAME)
        Mockito.`when`(tokenIssuer.issueRefreshToken(subject)).thenReturn(REFRESH_TOKEN)
        Mockito.`when`(tokenIssuer.issueAccessToken(subject)).thenReturn(ACCESS_TOKEN)

        val result = LoginSessionIssuingService(tokenIssuer, refreshTokenStore).issueFor(MEMBER_ID, ROLE_NAME)

        result shouldBe LoginSession(ACCESS_TOKEN, REFRESH_TOKEN)
        val order = Mockito.inOrder(tokenIssuer, refreshTokenStore)
        order.verify(tokenIssuer).issueRefreshToken(subject)
        order.verify(refreshTokenStore).save(MEMBER_ID, REFRESH_TOKEN)
        order.verify(tokenIssuer).issueAccessToken(subject)
    }
})

private const val MEMBER_ID = 10L
private const val ROLE_NAME = "ROLE_MEMBER"
private const val ACCESS_TOKEN = "access-token"
private const val REFRESH_TOKEN = "refresh-token"
