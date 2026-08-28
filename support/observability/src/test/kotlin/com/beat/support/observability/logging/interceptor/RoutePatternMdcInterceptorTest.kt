package com.beat.support.observability.logging.interceptor

import com.beat.support.observability.logging.filter.BaseMdcLoggingFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class RoutePatternMdcInterceptorTest : FunSpec() {

    private val interceptor = RoutePatternMdcInterceptor()

    init {
        afterTest { MDC.clear() }

        test("method와 best matching route pattern을 MDC에 저장한다") {
            val request = MockHttpServletRequest("GET", "/api/performances/detail/19")
            request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/api/performances/detail/{performanceId}",
            )

            interceptor.preHandle(request, MockHttpServletResponse(), Any())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe
                "GET /api/performances/detail/{performanceId}"
        }

        test("handler mapping pattern이 없으면 no route fallback을 사용한다") {
            val request = MockHttpServletRequest("GET", "/scanner/no-match")

            interceptor.preHandle(request, MockHttpServletResponse(), Any())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe
                BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN
        }

        test("afterCompletion은 MDC에서 route pattern을 제거하지 않는다") {
            // MDC cleanup is the sole responsibility of BaseMdcLoggingFilter.doFilterInternal
            // finally.
            // The interceptor must leave routePattern intact so the access log (emitted in filter
            // finally, which runs AFTER interceptor afterCompletion) can include the route field.
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
            MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/main")

            interceptor.afterCompletion(
                MockHttpServletRequest(),
                MockHttpServletResponse(),
                Any(),
                null,
            )

            MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe "trace-123"
            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY).shouldNotBeNull()
            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe "GET /api/main"
        }
    }
}
