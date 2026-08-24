package com.beat.support.security.authentication.internal

import com.beat.support.security.jwt.internal.AccessTokenAuthenticationFailure
import com.beat.support.security.jwt.internal.AccessTokenAuthenticationResult
import com.beat.support.security.jwt.internal.AccessTokenAuthenticator
import com.beat.support.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest : FunSpec() {
    init {
        afterEach { tearDown() }

        test("유효한 토큰은 SecurityContext와 이미 초기화된 MDC userId를 갱신한다") {
            val (accessTokenAuthenticator, filter) = jwtFilterDependencies()
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
            every { accessTokenAuthenticator.authenticateAccessToken("valid-token") } returns
                AccessTokenAuthenticationResult.Authenticated(42L, "ROLE_MEMBER")
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
            verify { accessTokenAuthenticator.authenticateAccessToken("valid-token") }
        }

        test("ADMIN 토큰은 관리자 Authentication으로 인증된다") {
            val (accessTokenAuthenticator, filter) = jwtFilterDependencies()
            every { accessTokenAuthenticator.authenticateAccessToken("admin-token") } returns
                AccessTokenAuthenticationResult.Authenticated(7L, "ROLE_ADMIN")
            val request = requestWithBearer("admin-token")
            val response = MockHttpServletResponse()
            val chain = FilterChain { _, _ ->
                val authentication = SecurityContextHolder.getContext().authentication
                    ?: error("ADMIN authentication이 생성되어야 한다")

                authentication::class.simpleName shouldBe "AdminAuthentication"
                authentication.principal shouldBe 7L
                authentication.authorities.single().authority shouldBe "ROLE_ADMIN"
            }

            filter.doFilter(request, response, chain)

            response.status shouldBe HttpServletResponse.SC_OK
        }

        test("서명 검증을 통과해도 USER와 unknown role은 401로 거부된다") {
            val (accessTokenAuthenticator, filter) = jwtFilterDependencies()
            listOf("ROLE_USER", "ROLE_UNKNOWN").forEach { role ->
                val token = role.lowercase()
                every { accessTokenAuthenticator.authenticateAccessToken(token) } returns
                    AccessTokenAuthenticationResult.Authenticated(42L, role)
                val request = requestWithBearer(token)
                val response = MockHttpServletResponse()
                val chain = mockk<FilterChain>(relaxed = true)

                filter.doFilter(request, response, chain)

                response.status shouldBe HttpServletResponse.SC_UNAUTHORIZED
                SecurityContextHolder.getContext().authentication shouldBe null
                verify(exactly = 0) { chain.doFilter(request, response) }
            }
        }

        test("만료된 토큰은 401로 단축 응답하고 기존 MDC를 유지한다") {
            val (accessTokenAuthenticator, filter) = jwtFilterDependencies()
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
            every { accessTokenAuthenticator.authenticateAccessToken("expired-token") } returns
                AccessTokenAuthenticationResult.Rejected(AccessTokenAuthenticationFailure.EXPIRED)
            val request = requestWithBearer("expired-token")
            val response = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(request, response, chain)

            response.status shouldBe HttpServletResponse.SC_UNAUTHORIZED
            MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe "trace-123"
            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_GUEST_USER
            SecurityContextHolder.getContext().authentication shouldBe null
            verify(exactly = 0) { chain.doFilter(request, response) }
        }

        test("유효하지 않은 토큰은 400으로 단축 응답한다") {
            val (accessTokenAuthenticator, filter) = jwtFilterDependencies()
            every { accessTokenAuthenticator.authenticateAccessToken("broken-token") } returns
                AccessTokenAuthenticationResult.Rejected(AccessTokenAuthenticationFailure.INVALID_SIGNATURE)
            val response = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(requestWithBearer("broken-token"), response, chain)

            response.status shouldBe HttpServletResponse.SC_BAD_REQUEST
            SecurityContextHolder.getContext().authentication shouldBe null
        }

        test("Authorization 헤더가 없으면 인증 없이 체인을 통과시킨다") {
            val (accessTokenAuthenticator, filter) = jwtFilterDependencies()
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

private data class JwtFilterDependencies(
    val accessTokenAuthenticator: AccessTokenAuthenticator = mockk(relaxed = true),
    val filter: JwtAuthenticationFilter = JwtAuthenticationFilter(accessTokenAuthenticator),
)

private fun jwtFilterDependencies() = JwtFilterDependencies()
