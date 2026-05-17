package com.beat.observability.logging.interceptor

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class RoutePatternMdcInterceptorTest {

    private val interceptor = RoutePatternMdcInterceptor()

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `stores method and best matching route pattern in MDC`() {
        val request = MockHttpServletRequest("GET", "/api/performances/detail/19")
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/performances/detail/{performanceId}")

        interceptor.preHandle(request, MockHttpServletResponse(), Any())

        assertEquals(
            "GET /api/performances/detail/{performanceId}",
            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY),
        )
    }

    @Test
    fun `uses no route fallback when handler mapping pattern is unavailable`() {
        val request = MockHttpServletRequest("GET", "/scanner/no-match")

        interceptor.preHandle(request, MockHttpServletResponse(), Any())

        assertEquals(BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN, MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    @Test
    fun `removes route pattern after handler completion without clearing other MDC keys`() {
        MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
        MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/main")

        interceptor.afterCompletion(MockHttpServletRequest(), MockHttpServletResponse(), Any(), null)

        assertEquals("trace-123", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
        assertNull(MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }
}
