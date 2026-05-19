package com.beat.observability.logging.filter

import com.beat.observability.logging.access.AccessLogEmitter
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

class BaseMdcLoggingFilterTest {

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

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
        val response = MockHttpServletResponse()
        val request = request().apply {
            addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "trace id with spaces")
        }

        testFilter(null).doFilter(request, response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertEquals(traceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        }
        assertMdcCleared()
    }

    @Test
    fun `generates trace id when request id header is too long`() {
        val response = MockHttpServletResponse()
        val request = request().apply {
            addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "a".repeat(129))
        }

        testFilter(null).doFilter(request, response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
        }
        assertMdcCleared()
    }

    @Test
    fun `generates trace id when request id header is missing`() {
        val response = MockHttpServletResponse()
        testFilter(null).doFilter(request(), response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
        }
        assertMdcCleared()
    }

    @Test
    fun `uses first forwarded for ip before real ip and remote addr`() {
        val request = request().apply {
            addHeader(BaseMdcLoggingFilter.X_FORWARDED_FOR_HEADER, "10.0.0.1, 10.0.0.2")
            addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")
        }

        testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("10.0.0.1", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }
        assertMdcCleared()
    }

    @Test
    fun `uses real ip when forwarded for is missing`() {
        val request = request().apply { addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3") }

        testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("10.0.0.3", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }
        assertMdcCleared()
    }

    @Test
    fun `uses remote addr when proxy headers are missing`() {
        val request = request().apply { remoteAddr = "127.0.0.1" }

        testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("127.0.0.1", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }
        assertMdcCleared()
    }

    @Test
    fun `stores request info and falls back to guest user when resolver returns blank`() {
        testFilter(" ").doFilter(request(method = "POST", uri = "/api/bookings"), MockHttpServletResponse()) { _, _ ->
            assertEquals("POST /api/bookings", MDC.get(BaseMdcLoggingFilter.REQUEST_INFO_KEY))
            assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        }
        assertMdcCleared()
    }

    @Test
    fun `uses resolved traceId and spanId when active span is available`() {
        val otelTraceId = "abcdef0123456789abcdef0123456789"
        val otelSpanId = "fedcba9876543210"
        val response = MockHttpServletResponse()

        testFilter(null, stubResolver(otelTraceId, otelSpanId)).doFilter(request(), response) { _, _ ->
            assertEquals(otelTraceId, MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
            assertEquals(otelSpanId, MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY))
        }
        assertEquals(otelTraceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        assertMdcCleared()
    }

    @Test
    fun `falls back to UUID traceId and omits spanId when resolver returns null`() {
        testFilter(null, NoOpTraceContextResolver).doFilter(request(), MockHttpServletResponse()) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertNull(MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY))
        }
        assertMdcCleared()
    }

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
        MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, "stale")
        testFilter("  ").refreshUserIdInMdc()
        assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
    }

    @Test
    fun `exception thrown by filter chain is stored as request attribute and rethrown`() {
        val cause = RuntimeException("db timeout")
        val request = request()

        assertThrows<RuntimeException> {
            testFilter(null).doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> throw cause })
        }

        assertSame(cause, request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR))
        assertMdcCleared()
    }

    @Test
    fun `MDC is cleared even when filter chain throws`() {
        runCatching {
            testFilter(null).doFilter(request(), MockHttpServletResponse(), FilterChain { _, _ -> throw RuntimeException() })
        }
        assertMdcCleared()
    }

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
        doFilter(request, response, FilterChain { req, res ->
            assertion(req as MockHttpServletRequest, res as MockHttpServletResponse)
        })
    }

    private fun assertMdcCleared() {
        assertTrue(MDC.getCopyOfContextMap().isNullOrEmpty())
    }
}
