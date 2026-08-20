package com.beat.support.security.authentication.internal

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import com.beat.observability.tracing.NoOpTraceContextResolver
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class SecurityMdcLoggingFilterTest {

    private val filter = SecurityMdcLoggingFilter(NoOpTraceContextResolver)
    private val filterWithManagementPort = SecurityMdcLoggingFilter(NoOpTraceContextResolver, MANAGEMENT_PORT)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
        MDC.clear()
    }

    @Test
    fun `SecurityContext가 비어 있으면 GUEST로 기록한다`() {
        assertUserIdDuringChain(BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
    }

    @Test
    fun `Long principal은 userId로 사용한다`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(123L, null, AuthorityUtils.createAuthorityList("ROLE_MEMBER"))

        assertUserIdDuringChain("123")
    }

    @Test
    fun `익명 인증은 GUEST로 기록한다`() {
        SecurityContextHolder.getContext().authentication = AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
        )

        assertUserIdDuringChain(BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
    }

    @Test
    fun `Long principal이 아니면 authentication name으로 대체한다`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            "member-name",
            null,
            AuthorityUtils.createAuthorityList("ROLE_MEMBER"),
        )

        assertUserIdDuringChain("member-name")
    }

    @Test
    fun `management port 요청은 필터를 건너뛴다`() {
        val request = MockHttpServletRequest("GET", "/actuator/prometheus").apply { setLocalPort(MANAGEMENT_PORT) }
        var chainInvoked = false
        var mdcPopulatedDuringChain = false
        val chain = FilterChain { _, _ ->
            chainInvoked = true
            mdcPopulatedDuringChain = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null
        }

        filterWithManagementPort.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chainInvoked)
        assertFalse(mdcPopulatedDuringChain)
        assertTrue(MDC.getCopyOfContextMap().isNullOrEmpty())
    }

    @Test
    fun `일반 port 요청은 필터를 건너뛰지 않는다`() {
        val request = MockHttpServletRequest("GET", "/api/main").apply { setLocalPort(8080) }
        var mdcPopulated = false
        val chain = FilterChain { _, _ -> mdcPopulated = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null }

        filterWithManagementPort.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(mdcPopulated)
    }

    @Test
    fun `management port가 주입되지 않으면 어떤 요청도 건너뛰지 않는다`() {
        val request = MockHttpServletRequest("GET", "/actuator/prometheus").apply { setLocalPort(MANAGEMENT_PORT) }
        var mdcPopulated = false
        val chain = FilterChain { _, _ -> mdcPopulated = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(mdcPopulated)
    }

    private fun assertUserIdDuringChain(expectedUserId: String) {
        val chain = FilterChain { _, _ ->
            assertEquals(expectedUserId, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        }

        filter.doFilter(MockHttpServletRequest("GET", "/api/main"), MockHttpServletResponse(), chain)

        assertTrue(MDC.getCopyOfContextMap().isNullOrEmpty())
    }

    companion object {
        private const val MANAGEMENT_PORT = 55555
    }
}
