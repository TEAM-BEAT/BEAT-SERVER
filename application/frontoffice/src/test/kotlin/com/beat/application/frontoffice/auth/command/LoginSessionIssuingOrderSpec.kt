package com.beat.application.frontoffice.auth.command

import com.beat.application.frontoffice.security.TokenIssuer
import com.beat.application.frontoffice.security.TokenSubject
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder

class LoginSessionIssuingOrderSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("access token 발급 전에 refresh token을 저장하고 세션을 반환한다") {
        val tokenIssuer = mockk<TokenIssuer>(relaxed = true)
        val refreshTokenStore = mockk<RefreshTokenStore>(relaxed = true)
        val subject = TokenSubject(MEMBER_ID, ROLE_NAME)
        every { tokenIssuer.issueRefreshToken(subject) } returns REFRESH_TOKEN
        every { tokenIssuer.issueAccessToken(subject) } returns ACCESS_TOKEN

        val result = LoginSessionIssuingService(tokenIssuer, refreshTokenStore).issueFor(MEMBER_ID, ROLE_NAME)

        result shouldBe LoginSession(ACCESS_TOKEN, REFRESH_TOKEN)
        verifyOrder {
            tokenIssuer.issueRefreshToken(subject)
            refreshTokenStore.save(MEMBER_ID, REFRESH_TOKEN)
            tokenIssuer.issueAccessToken(subject)
        }
    }
})

private const val MEMBER_ID = 10L
private const val ROLE_NAME = "ROLE_MEMBER"
private const val ACCESS_TOKEN = "access-token"
private const val REFRESH_TOKEN = "refresh-token"
