package com.beat.support.security.authentication.internal

import com.beat.support.observability.logging.filter.BaseMdcLoggingFilter
import com.beat.support.observability.tracing.NoOpTraceContextResolver
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.FilterChain
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder

class SecurityMdcLoggingFilterTest : FunSpec() {

    private val filter = SecurityMdcLoggingFilter(NoOpTraceContextResolver)
    private val filterWithManagementPort =
        SecurityMdcLoggingFilter(NoOpTraceContextResolver, MANAGEMENT_PORT)

    init {
        isolationMode = IsolationMode.SingleInstance

        afterEach { clearContext() }

        test("SecurityContext가 비어 있으면 GUEST로 기록한다") {
            assertUserIdDuringChain(BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
        }

        test("Long principal은 userId로 사용한다") {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                    123L,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_MEMBER"),
                )

            assertUserIdDuringChain("123")
        }

        test("익명 인증은 GUEST로 기록한다") {
            SecurityContextHolder.getContext().authentication =
                AnonymousAuthenticationToken(
                    "key",
                    "anonymousUser",
                    AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
                )

            assertUserIdDuringChain(BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
        }

        test("Long principal이 아니면 authentication name으로 대체한다") {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                    "member-name",
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_MEMBER"),
                )

            assertUserIdDuringChain("member-name")
        }

        test("management port 요청은 필터를 건너뛴다") {
            val request =
                MockHttpServletRequest("GET", "/actuator/prometheus").apply {
                    setLocalPort(MANAGEMENT_PORT)
                }
            var chainInvoked = false
            var mdcPopulatedDuringChain = false
            val chain = FilterChain { _, _ ->
                chainInvoked = true
                mdcPopulatedDuringChain = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null
            }

            filterWithManagementPort.doFilter(request, MockHttpServletResponse(), chain)

            chainInvoked shouldBe true
            mdcPopulatedDuringChain shouldBe false
            MDC.getCopyOfContextMap().isNullOrEmpty() shouldBe true
        }

        test("일반 port 요청은 필터를 건너뛰지 않는다") {
            val request = MockHttpServletRequest("GET", "/api/main").apply { setLocalPort(8080) }
            var mdcPopulated = false
            val chain = FilterChain { _, _ ->
                mdcPopulated = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null
            }

            filterWithManagementPort.doFilter(request, MockHttpServletResponse(), chain)

            mdcPopulated shouldBe true
        }

        test("management port가 주입되지 않으면 어떤 요청도 건너뛰지 않는다") {
            val request =
                MockHttpServletRequest("GET", "/actuator/prometheus").apply {
                    setLocalPort(MANAGEMENT_PORT)
                }
            var mdcPopulated = false
            val chain = FilterChain { _, _ ->
                mdcPopulated = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null
            }

            filter.doFilter(request, MockHttpServletResponse(), chain)

            mdcPopulated shouldBe true
        }
    }

    private fun assertUserIdDuringChain(expectedUserId: String) {
        val chain = FilterChain { _, _ ->
            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe expectedUserId
        }

        filter.doFilter(
            MockHttpServletRequest("GET", "/api/main"),
            MockHttpServletResponse(),
            chain,
        )

        MDC.getCopyOfContextMap().isNullOrEmpty() shouldBe true
    }

    private fun clearContext() {
        SecurityContextHolder.clearContext()
        MDC.clear()
    }

    companion object {
        private const val MANAGEMENT_PORT = 55555
    }
}
