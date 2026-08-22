package com.beat.observability.logging.filter

import com.beat.observability.logging.access.AccessLogEmitter
import com.beat.observability.tracing.NoOpTraceContextResolver
import com.beat.observability.tracing.TraceContextResolver
import com.beat.observability.tracing.TraceContextResolver.ResolvedTraceContext
import jakarta.servlet.FilterChain
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class BaseMdcLoggingFilterTest : FunSpec() {

    init {
        afterTest { MDC.clear() }

        test("uses sanitized request id header as trace id and writes it to response") {
            val filter = testFilter("42")
            val request = request()
            request.addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, " trace-from-client_1.2:3 ")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe "trace-from-client_1.2:3"
                MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe "42"
            }

            response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER) shouldBe "trace-from-client_1.2:3"
            assertMdcCleared()
        }

        test("generates trace id when request id header contains unsupported characters") {
            val response = MockHttpServletResponse()
            val request = request().apply {
                addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "trace id with spaces")
            }

            testFilter(null).doFilter(request, response) { _, _ ->
                val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldNotBeNull()
                traceId.length shouldBe 32
                response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER) shouldBe traceId
            }
            assertMdcCleared()
        }

        test("generates trace id when request id header is too long") {
            val response = MockHttpServletResponse()
            val request = request().apply {
                addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "a".repeat(129))
            }

            testFilter(null).doFilter(request, response) { _, _ ->
                val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldNotBeNull()
                traceId.length shouldBe 32
            }
            assertMdcCleared()
        }

        test("generates trace id when request id header is missing") {
            val response = MockHttpServletResponse()
            testFilter(null).doFilter(request(), response) { _, _ ->
                val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldNotBeNull()
                traceId.length shouldBe 32
            }
            assertMdcCleared()
        }

        test("uses first forwarded for ip before real ip and remote addr") {
            val request = request().apply {
                addHeader(BaseMdcLoggingFilter.X_FORWARDED_FOR_HEADER, "10.0.0.1, 10.0.0.2")
                addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")
            }

            testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY) shouldBe "10.0.0.1"
            }
            assertMdcCleared()
        }

        test("uses real ip when forwarded for is missing") {
            val request = request().apply { addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3") }

            testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY) shouldBe "10.0.0.3"
            }
            assertMdcCleared()
        }

        test("uses remote addr when proxy headers are missing") {
            val request = request().apply { remoteAddr = "127.0.0.1" }

            testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY) shouldBe "127.0.0.1"
            }
            assertMdcCleared()
        }

        test("stores request info and falls back to guest user when resolver returns blank") {
            testFilter(" ").doFilter(request(method = "POST", uri = "/api/bookings"), MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.REQUEST_INFO_KEY) shouldBe "POST /api/bookings"
                MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_GUEST_USER
            }
            assertMdcCleared()
        }

        test("uses resolved traceId and spanId when active span is available") {
            val otelTraceId = "abcdef0123456789abcdef0123456789"
            val otelSpanId = "fedcba9876543210"
            val response = MockHttpServletResponse()

            testFilter(null, stubResolver(otelTraceId, otelSpanId)).doFilter(request(), response) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe otelTraceId
                MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY) shouldBe otelSpanId
            }
            response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER) shouldBe otelTraceId
            assertMdcCleared()
        }

        test("falls back to UUID traceId and omits spanId when resolver returns null") {
            testFilter(null, NoOpTraceContextResolver).doFilter(request(), MockHttpServletResponse()) { _, _ ->
                val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldNotBeNull()
                traceId.length shouldBe 32
                MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY).shouldBeNull()
            }
            assertMdcCleared()
        }

        test("refreshUserIdInMdc updates MDC with current resolved userId") {
            var resolvedId: String? = null
            val filter = object : BaseMdcLoggingFilter(NoOpTraceContextResolver) {
                override fun resolveUserId(): String? = resolvedId
            }

            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
            resolvedId = "99"
            filter.refreshUserIdInMdc()

            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe "99"
        }

        test("refreshUserIdInMdc falls back to GUEST when resolveUserId returns blank") {
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, "stale")
            testFilter("  ").refreshUserIdInMdc()
            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_GUEST_USER
        }

        test("exception thrown by filter chain is stored as request attribute and rethrown") {
            val cause = RuntimeException("db timeout")
            val request = request()

            shouldThrow<RuntimeException> {
                testFilter(null).doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> throw cause })
            }

            request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR) shouldBeSameInstanceAs cause
            assertMdcCleared()
        }

        test("MDC is cleared even when filter chain throws") {
            runCatching {
                testFilter(null).doFilter(request(), MockHttpServletResponse(), FilterChain { _, _ -> throw RuntimeException() })
            }
            assertMdcCleared()
        }
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
        MDC.getCopyOfContextMap().isNullOrEmpty() shouldBe true
    }
}
