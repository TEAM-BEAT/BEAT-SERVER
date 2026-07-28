package com.beat.gateway.authentication.internal

import com.beat.contracts.auth.JwtTokenPort
import com.beat.contracts.auth.JwtTokenType
import com.beat.contracts.auth.TokenValidationResult
import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.mockito.Mockito.`when` as given

class JwtAuthenticationFilterTest {

    private val jwtTokenPort = mock(JwtTokenPort::class.java)
    private val filter = JwtAuthenticationFilter(jwtTokenPort)

    @AfterEach
    fun tearDown() {
        MDC.clear()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효한 토큰은 SecurityContext와 이미 초기화된 MDC userId를 갱신한다`() {
        MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
        MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
        given(jwtTokenPort.validateAccessToken("valid-token")).thenReturn(TokenValidationResult.VALID)
        given(jwtTokenPort.getMemberId("valid-token", JwtTokenType.ACCESS)).thenReturn(42L)
        given(jwtTokenPort.getRoleName("valid-token", JwtTokenType.ACCESS)).thenReturn("ROLE_MEMBER")
        val request = requestWithBearer("valid-token")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ ->
            assertEquals("trace-123", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
            assertEquals("42", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
            assertNotNull(SecurityContextHolder.getContext().authentication)
        }

        filter.doFilter(request, response, chain)

        assertEquals("42", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        assertEquals(HttpServletResponse.SC_OK, response.status)
    }

    @Test
    fun `만료된 토큰은 401로 단축 응답하고 기존 MDC를 유지한다`() {
        MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
        MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
        given(jwtTokenPort.validateAccessToken("expired-token")).thenReturn(TokenValidationResult.EXPIRED)
        val request = requestWithBearer("expired-token")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
        assertEquals("trace-123", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
        assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `유효하지 않은 토큰은 400으로 단축 응답한다`() {
        given(jwtTokenPort.validateAccessToken("broken-token"))
            .thenReturn(TokenValidationResult.INVALID_SIGNATURE)
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(requestWithBearer("broken-token"), response, chain)

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Authorization 헤더가 없으면 인증 없이 체인을 통과시킨다`() {
        val response = MockHttpServletResponse()
        var chainInvoked = false
        val chain = FilterChain { _, _ -> chainInvoked = true }

        filter.doFilter(MockHttpServletRequest("GET", "/api/main"), response, chain)

        assertEquals(true, chainInvoked)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    private fun requestWithBearer(token: String): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/api/main").apply {
            addHeader("Authorization", "Bearer $token")
        }
}
