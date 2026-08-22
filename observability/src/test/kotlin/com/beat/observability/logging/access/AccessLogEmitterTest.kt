package com.beat.observability.logging.access

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
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

        test("emit sets status and elapsed MDC fields") {
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

        test("emit falls back to request attribute when MDC route pattern is absent") {
            val request = request(uri = "/api/concerts/1").apply {
                setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
                setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime())
            }
            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY).shouldBeNull()

            emitter.emit(request, MockHttpServletResponse())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe "GET /api/concerts/{id}"
        }

        test("emit uses DEFAULT_ROUTE_PATTERN when neither MDC nor request attribute has route") {
            val request = request().apply { setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime()) }

            emitter.emit(request, MockHttpServletResponse())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN
        }

        test("emit keeps interceptor-set route pattern over request attribute fallback") {
            val request = request(uri = "/api/concerts/1").apply {
                setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
                setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime())
            }
            MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/concerts/{concertId}")

            emitter.emit(request, MockHttpServletResponse())

            MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe "GET /api/concerts/{concertId}"
        }

        test("shouldEmit returns false for OPTIONS request") {
            emitter.shouldEmit(request(method = "OPTIONS")) shouldBe false
        }

        test("shouldEmit returns false for actuator health prefix paths") {
            listOf("/actuator/health", "/actuator/health/liveness", "/actuator/health/customGroup").forEach { path ->
                emitter.shouldEmit(request(uri = path)) shouldBe false
            }
        }

        test("shouldEmit returns true for ordinary GET request") {
            emitter.shouldEmit(request()) shouldBe true
        }

        test("markStart records nanos on request attribute") {
            val request = request()
            emitter.markStart(request)
            (request.getAttribute(AccessLogEmitter.START_NANOS_ATTR) as? Long).shouldNotBeNull()
        }
    }

    private fun request(method: String = "GET", uri: String = "/api/main"): MockHttpServletRequest =
        MockHttpServletRequest(method, uri)
}
