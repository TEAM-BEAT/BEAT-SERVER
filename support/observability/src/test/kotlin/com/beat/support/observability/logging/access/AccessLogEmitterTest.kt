package com.beat.support.observability.logging.access

import com.beat.support.observability.logging.filter.BaseMdcLoggingFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class AccessLogEmitterTest : FunSpec() {

    private val emitter = AccessLogEmitter()

    init {
        afterTest { MDC.clear() }

        test("emit은 status와 elapsed MDC 필드를 설정한다") {
            val request = request().apply {
                setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime() - 10_000_000L)
            }
            val response = MockHttpServletResponse().apply { status = 200 }

            emitter.emit(request, response)

            MDC.get(AccessLogEmitter.STATUS_KEY) shouldBe "200"
            val elapsed = MDC.get(AccessLogEmitter.ELAPSED_KEY)?.toLongOrNull()
            elapsed.shouldNotBeNull()
            (elapsed >= 0) shouldBe true
        }

        test("MDC route pattern이 없으면 emit은 request attribute로 fallback한다") {
            val request = request(uri = "/api/concerts/1").apply {
                setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
                setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime())
            }
            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY).shouldBeNull()

            emitter.emit(request, MockHttpServletResponse())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe "GET /api/concerts/{id}"
        }

        test("MDC와 request attribute 모두 route가 없으면 emit은 DEFAULT_ROUTE_PATTERN을 사용한다") {
            val request = request().apply { setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime()) }

            emitter.emit(request, MockHttpServletResponse())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN
        }

        test("emit은 interceptor가 설정한 route pattern을 request attribute fallback보다 우선 사용한다") {
            val request = request(uri = "/api/concerts/1").apply {
                setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
                setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime())
            }
            MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/concerts/{concertId}")

            emitter.emit(request, MockHttpServletResponse())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe "GET /api/concerts/{concertId}"
        }

        test("OPTIONS request에 대해 shouldEmit은 false를 반환한다") {
            emitter.shouldEmit(request(method = "OPTIONS")) shouldBe false
        }

        test("일반 GET request에 대해 shouldEmit은 true를 반환한다") {
            emitter.shouldEmit(request()) shouldBe true
        }

        test("markStart는 nano timing을 request attribute에 기록한다") {
            val request = request()
            emitter.markStart(request)
            (request.getAttribute(AccessLogEmitter.START_NANOS_ATTR) as? Long).shouldNotBeNull()
        }
    }

    private fun request(method: String = "GET", uri: String = "/api/main"): MockHttpServletRequest =
        MockHttpServletRequest(method, uri)
}
