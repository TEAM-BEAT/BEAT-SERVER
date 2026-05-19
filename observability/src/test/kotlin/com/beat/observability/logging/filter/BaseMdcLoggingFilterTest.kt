package com.beat.observability.logging.filter

import com.beat.observability.tracing.NoOpTraceContextResolver
import com.beat.observability.tracing.TraceContextResolver
import com.beat.observability.tracing.TraceContextResolver.ResolvedTraceContext
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class BaseMdcLoggingFilterTest {

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    // ── MDC population ────────────────────────────────────────────────────────

    @Test
    fun `uses sanitized request id header as trace id and writes it to response`() {
        val filter = testFilter("42")
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, " trace-from-client_1.2:3 ")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, _ ->
            assertEquals("trace-from-client_1.2:3", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
            assertEquals("42", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        }

        assertEquals("trace-from-client_1.2:3", response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        assertMdcCleared()
    }

    @Test
    fun `generates trace id when request id header contains unsupported characters`() {
        val filter = testFilter(null)
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "trace id with spaces")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertEquals(traceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        }

        assertMdcCleared()
    }

    @Test
    fun `generates trace id when request id header is too long`() {
        val filter = testFilter(null)
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "a".repeat(129))
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertEquals(traceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        }

        assertMdcCleared()
    }

    @Test
    fun `generates trace id when request id header is missing`() {
        val filter = testFilter(null)
        val request = request()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertEquals(traceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses first forwarded for ip before real ip and remote addr`() {
        val filter = testFilter(null)
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.X_FORWARDED_FOR_HEADER, "10.0.0.1, 10.0.0.2")
        request.addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("10.0.0.1", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses real ip when forwarded for is missing`() {
        val filter = testFilter(null)
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("10.0.0.3", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses remote addr when proxy headers are missing`() {
        val filter = testFilter(null)
        val request = request()
        request.remoteAddr = "127.0.0.1"

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("127.0.0.1", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `stores request info and falls back to guest user`() {
        val filter = testFilter(" ")
        val request = request(method = "POST", uri = "/api/bookings")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("POST /api/bookings", MDC.get(BaseMdcLoggingFilter.REQUEST_INFO_KEY))
            assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses resolved traceId and spanId when active span is available`() {
        val otelTraceId = "abcdef0123456789abcdef0123456789"
        val otelSpanId = "fedcba9876543210"
        val filter = testFilter(null, stubResolver(otelTraceId, otelSpanId))
        val response = MockHttpServletResponse()

        filter.doFilter(request(), response) { _, _ ->
            assertEquals(otelTraceId, MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
            assertEquals(otelSpanId, MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY))
        }

        assertEquals(otelTraceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        assertMdcCleared()
    }

    @Test
    fun `falls back to UUID traceId and omits spanId when resolver returns null`() {
        val filter = testFilter(null, NoOpTraceContextResolver)

        filter.doFilter(request(), MockHttpServletResponse()) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertNull(MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY))
        }

        assertMdcCleared()
    }

    // ── emitAccessLog ─────────────────────────────────────────────────────────

    @Test
    fun `emitAccessLog sets status and elapsed MDC fields`() {
        val filter = testFilter(null)
        val request = request()
        request.setAttribute(BaseMdcLoggingFilter.START_NANOS_ATTR, System.nanoTime() - 10_000_000L)
        val response = MockHttpServletResponse().apply { status = 200 }

        filter.emitAccessLog(request, response)

        assertEquals("200", MDC.get(BaseMdcLoggingFilter.STATUS_KEY))
        val elapsed = MDC.get(BaseMdcLoggingFilter.ELAPSED_KEY)?.toLongOrNull()
        assertNotNull(elapsed)
        assertTrue(elapsed!! >= 0)
    }

    @Test
    fun `emitAccessLog falls back to request attribute when MDC route pattern is absent`() {
        val filter = testFilter(null)
        val request = request(method = "GET", uri = "/api/concerts/1")
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
        request.setAttribute(BaseMdcLoggingFilter.START_NANOS_ATTR, System.nanoTime())
        val response = MockHttpServletResponse()

        assertNull(MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))

        filter.emitAccessLog(request, response)

        assertEquals("GET /api/concerts/{id}", MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    @Test
    fun `emitAccessLog uses DEFAULT_ROUTE_PATTERN when neither MDC nor request attribute has route`() {
        val filter = testFilter(null)
        val request = request()
        request.setAttribute(BaseMdcLoggingFilter.START_NANOS_ATTR, System.nanoTime())

        filter.emitAccessLog(request, MockHttpServletResponse())

        assertEquals(BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN, MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    @Test
    fun `emitAccessLog keeps interceptor-set route pattern over request attribute fallback`() {
        val filter = testFilter(null)
        val request = request(method = "GET", uri = "/api/concerts/1")
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
        request.setAttribute(BaseMdcLoggingFilter.START_NANOS_ATTR, System.nanoTime())

        MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/concerts/{concertId}")

        filter.emitAccessLog(request, MockHttpServletResponse())

        assertEquals("GET /api/concerts/{concertId}", MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    // ── Path / method skip ────────────────────────────────────────────────────

    @Test
    fun `OPTIONS request does not emit access log but MDC is still cleared`() {
        val filter = testFilter(null)
        val request = request(method = "OPTIONS", uri = "/api/performances")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ -> }

        assertMdcCleared()
        assertNull(MDC.get(BaseMdcLoggingFilter.STATUS_KEY))
    }

    @Test
    fun `actuator health path does not emit access log`() {
        val filter = testFilter(null)
        val request = request(method = "GET", uri = "/actuator/health")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ -> }

        assertMdcCleared()
        assertNull(MDC.get(BaseMdcLoggingFilter.STATUS_KEY))
    }

    // ── refreshUserIdInMdc ────────────────────────────────────────────────────

    @Test
    fun `refreshUserIdInMdc updates MDC with current resolved userId`() {
        var resolvedId: String? = null
        val filter = object : BaseMdcLoggingFilter(NoOpTraceContextResolver) {
            override fun resolveUserId(): String? = resolvedId
        }

        MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
        resolvedId = "99"

        filter.refreshUserIdInMdc()

        assertEquals("99", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
    }

    @Test
    fun `refreshUserIdInMdc falls back to GUEST when resolveUserId returns blank`() {
        val filter = testFilter("  ")

        MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, "stale")
        filter.refreshUserIdInMdc()

        assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
    }

    // ── Exception handling ────────────────────────────────────────────────────

    @Test
    fun `exception thrown by filter chain is stored as request attribute and rethrown`() {
        val cause = RuntimeException("db timeout")
        val filter = testFilter(null)
        val request = request()
        val response = MockHttpServletResponse()

        assertThrows<RuntimeException> {
            filter.doFilter(request, response, FilterChain { _, _ -> throw cause })
        }

        assertSame(cause, request.getAttribute(BaseMdcLoggingFilter.EXCEPTION_ATTR))
        assertMdcCleared()
    }

    @Test
    fun `MDC is cleared even when filter chain throws`() {
        val filter = testFilter(null)

        runCatching {
            filter.doFilter(request(), MockHttpServletResponse(), FilterChain { _, _ -> throw RuntimeException() })
        }

        assertMdcCleared()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubResolver(traceId: String, spanId: String): TraceContextResolver =
        TraceContextResolver { ResolvedTraceContext(traceId, spanId) }

    private fun testFilter(
        userId: String?,
        resolver: TraceContextResolver = NoOpTraceContextResolver,
    ): BaseMdcLoggingFilter =
        object : BaseMdcLoggingFilter(resolver) {
            override fun resolveUserId(): String? = userId
        }

    private fun request(method: String = "GET", uri: String = "/api/main"): MockHttpServletRequest =
        MockHttpServletRequest(method, uri)

    private fun BaseMdcLoggingFilter.doFilter(
        request: MockHttpServletRequest,
        response: MockHttpServletResponse,
        assertion: (MockHttpServletRequest, MockHttpServletResponse) -> Unit,
    ) {
        val chain = FilterChain { servletRequest, servletResponse ->
            assertion(servletRequest as MockHttpServletRequest, servletResponse as MockHttpServletResponse)
        }
        doFilter(request, response, chain)
    }

    private fun assertMdcCleared() {
        assertTrue(MDC.getCopyOfContextMap().isNullOrEmpty())
        assertFalse(MDC.getCopyOfContextMap()?.containsKey(BaseMdcLoggingFilter.TRACE_ID_KEY) ?: false)
    }
}
