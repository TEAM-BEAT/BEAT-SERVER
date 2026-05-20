package com.beat.observability.logging.interceptor

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `afterCompletion does not remove route pattern from MDC`() {
        // MDC cleanup is the sole responsibility of BaseMdcLoggingFilter.doFilterInternal finally.
        // The interceptor must leave routePattern intact so the access log (emitted in filter
        // finally, which runs AFTER interceptor afterCompletion) can include the route field.
        MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
        MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/main")

        interceptor.afterCompletion(MockHttpServletRequest(), MockHttpServletResponse(), Any(), null)

        assertEquals("trace-123", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
        assertNotNull(
            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY),
            "routePattern must remain in MDC after afterCompletion — filter finally owns cleanup",
        )
        assertEquals("GET /api/main", MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }
}
