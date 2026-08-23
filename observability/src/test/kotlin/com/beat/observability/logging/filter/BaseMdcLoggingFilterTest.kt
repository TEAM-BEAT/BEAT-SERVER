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

        test("sanitized된 request id header를 trace id로 사용하고 response에 기록한다") {
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

        test("request id header가 지원하지 않는 문자를 포함하면 trace id를 생성한다") {
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

        test("request id header가 너무 길면 trace id를 생성한다") {
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

        test("request id header가 없으면 trace id를 생성한다") {
            val response = MockHttpServletResponse()
            testFilter(null).doFilter(request(), response) { _, _ ->
                val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldNotBeNull()
                traceId.length shouldBe 32
            }
            assertMdcCleared()
        }

        test("forwarded for ip 목록의 첫 번째 값을 real ip와 remote addr보다 우선 사용한다") {
            val request = request().apply {
                addHeader(BaseMdcLoggingFilter.X_FORWARDED_FOR_HEADER, "10.0.0.1, 10.0.0.2")
                addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")
            }

            testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY) shouldBe "10.0.0.1"
            }
            assertMdcCleared()
        }

        test("forwarded for가 없으면 real ip를 사용한다") {
            val request = request().apply { addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3") }

            testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY) shouldBe "10.0.0.3"
            }
            assertMdcCleared()
        }

        test("proxy header가 없으면 remote addr를 사용한다") {
            val request = request().apply { remoteAddr = "127.0.0.1" }

            testFilter(null).doFilter(request, MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY) shouldBe "127.0.0.1"
            }
            assertMdcCleared()
        }

        test("resolver가 blank를 반환하면 request info를 저장하고 guest user로 fallback한다") {
            testFilter(" ").doFilter(request(method = "POST", uri = "/api/bookings"), MockHttpServletResponse()) { _, _ ->
                MDC.get(BaseMdcLoggingFilter.REQUEST_INFO_KEY) shouldBe "POST /api/bookings"
                MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_GUEST_USER
            }
            assertMdcCleared()
        }

        test("active span이 있으면 resolver가 반환한 traceId와 spanId를 사용한다") {
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

        test("resolver가 null을 반환하면 UUID traceId로 fallback하고 spanId는 생략한다") {
            testFilter(null, NoOpTraceContextResolver).doFilter(request(), MockHttpServletResponse()) { _, _ ->
                val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldNotBeNull()
                traceId.length shouldBe 32
                MDC.get(BaseMdcLoggingFilter.SPAN_ID_KEY).shouldBeNull()
            }
            assertMdcCleared()
        }

        test("refreshUserIdInMdc는 현재 resolved userId로 MDC를 갱신한다") {
            var resolvedId: String? = null
            val filter = object : BaseMdcLoggingFilter(NoOpTraceContextResolver) {
                override fun resolveUserId(): String? = resolvedId
            }

            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER)
            resolvedId = "99"
            filter.refreshUserIdInMdc()

            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe "99"
        }

        test("resolveUserId가 blank를 반환하면 refreshUserIdInMdc는 GUEST로 fallback한다") {
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, "stale")
            testFilter("  ").refreshUserIdInMdc()
            MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_GUEST_USER
        }

        test("filter chain에서 던져진 exception은 request attribute에 저장된 뒤 다시 던져진다") {
            val cause = RuntimeException("db timeout")
            val request = request()

            shouldThrow<RuntimeException> {
                testFilter(null).doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> throw cause })
            }

            request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR) shouldBeSameInstanceAs cause
            assertMdcCleared()
        }

        test("filter chain이 예외를 던져도 MDC는 clear된다") {
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
