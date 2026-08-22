package com.beat.support.security.authentication.internal

import com.beat.support.security.jwt.internal.AccessTokenAuthenticator
import com.beat.support.security.token.TokenAuthenticationFailure
import com.beat.support.security.token.TokenAuthenticationResult
import com.beat.support.security.token.TokenSubject
import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.mockito.Mockito.`when` as given

class JwtAuthenticationFilterTest : FunSpec() {

    private val accessTokenAuthenticator = mock(AccessTokenAuthenticator::class.java)
    private val filter = JwtAuthenticationFilter(accessTokenAuthenticator)

    init {
        isolationMode = IsolationMode.SingleInstance

        afterEach { tearDown() }

        test("유효한 토큰은 SecurityContext와 이미 초기화된 MDC userId를 갱신한다") {
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
            given(accessTokenAuthenticator.authenticateAccessToken("valid-token"))
                .thenReturn(TokenAuthenticationResult.Authenticated(TokenSubject(42L, "ROLE_MEMBER")))
            val request = requestWithBearer("valid-token")
            val response = MockHttpServletResponse()
            val chain = FilterChain { _, _ ->
                MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe "trace-123"
                MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe "42"
                SecurityContextHolder.getContext().authentication shouldNotBe null
            }

            filter.doFilter(request, response, chain)

            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe "42"
            response.status shouldBe HttpServletResponse.SC_OK
            verify(accessTokenAuthenticator).authenticateAccessToken("valid-token")
        }

        test("만료된 토큰은 401로 단축 응답하고 기존 MDC를 유지한다") {
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
            given(accessTokenAuthenticator.authenticateAccessToken("expired-token"))
                .thenReturn(TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.EXPIRED))
            val request = requestWithBearer("expired-token")
            val response = MockHttpServletResponse()
            val chain = mock(FilterChain::class.java)

            filter.doFilter(request, response, chain)

            response.status shouldBe HttpServletResponse.SC_UNAUTHORIZED
            MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe "trace-123"
            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_GUEST_USER
            SecurityContextHolder.getContext().authentication shouldBe null
            verify(chain, never()).doFilter(request, response)
        }

        test("유효하지 않은 토큰은 400으로 단축 응답한다") {
            given(accessTokenAuthenticator.authenticateAccessToken("broken-token"))
                .thenReturn(TokenAuthenticationResult.Rejected(TokenAuthenticationFailure.INVALID_SIGNATURE))
            val response = MockHttpServletResponse()
            val chain = mock(FilterChain::class.java)

            filter.doFilter(requestWithBearer("broken-token"), response, chain)

            response.status shouldBe HttpServletResponse.SC_BAD_REQUEST
            SecurityContextHolder.getContext().authentication shouldBe null
        }

        test("Authorization 헤더가 없으면 인증 없이 체인을 통과시킨다") {
            val response = MockHttpServletResponse()
            var chainInvoked = false
            val chain = FilterChain { _, _ -> chainInvoked = true }

            filter.doFilter(MockHttpServletRequest("GET", "/api/main"), response, chain)

            chainInvoked shouldBe true
            SecurityContextHolder.getContext().authentication shouldBe null
        }
    }

    private fun tearDown() {
        MDC.clear()
        SecurityContextHolder.clearContext()
    }

    private fun requestWithBearer(token: String): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/api/main").apply {
            addHeader("Authorization", "Bearer $token")
        }
}
