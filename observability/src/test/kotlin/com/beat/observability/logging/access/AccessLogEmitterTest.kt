package com.beat.observability.logging.access

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class AccessLogEmitterTest {

    private val emitter = AccessLogEmitter()

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `emit sets status and elapsed MDC fields`() {
        val request = request().apply {
            setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime() - 10_000_000L)
        }
        val response = MockHttpServletResponse().apply { status = 200 }

        emitter.emit(request, response)

        assertEquals("200", MDC.get(AccessLogEmitter.STATUS_KEY))
        val elapsed = MDC.get(AccessLogEmitter.ELAPSED_KEY)?.toLongOrNull()
        assertNotNull(elapsed)
        assertTrue(elapsed!! >= 0)
    }

    @Test
    fun `emit falls back to request attribute when MDC route pattern is absent`() {
        val request = request(uri = "/api/concerts/1").apply {
            setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
            setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime())
        }
        assertNull(MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))

        emitter.emit(request, MockHttpServletResponse())

        assertEquals("GET /api/concerts/{id}", MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    @Test
    fun `emit uses DEFAULT_ROUTE_PATTERN when neither MDC nor request attribute has route`() {
        val request = request().apply { setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime()) }

        emitter.emit(request, MockHttpServletResponse())

        assertEquals(BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN, MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    @Test
    fun `emit keeps interceptor-set route pattern over request attribute fallback`() {
        val request = request(uri = "/api/concerts/1").apply {
            setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/concerts/{id}")
            setAttribute(AccessLogEmitter.START_NANOS_ATTR, System.nanoTime())
        }
        MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/concerts/{concertId}")

        emitter.emit(request, MockHttpServletResponse())

        assertEquals("GET /api/concerts/{concertId}", MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
    }

    @Test
    fun `shouldEmit returns false for OPTIONS request`() {
        assertFalse(emitter.shouldEmit(request(method = "OPTIONS")))
    }

    @Test
    fun `shouldEmit returns false for actuator health prefix paths`() {
        listOf("/actuator/health", "/actuator/health/liveness", "/actuator/health/customGroup").forEach { path ->
            assertFalse(emitter.shouldEmit(request(uri = path)), "should skip $path")
        }
    }

    @Test
    fun `shouldEmit returns true for ordinary GET request`() {
        assertTrue(emitter.shouldEmit(request()))
    }

    @Test
    fun `markStart records nanos on request attribute`() {
        val request = request()
        emitter.markStart(request)
        assertNotNull(request.getAttribute(AccessLogEmitter.START_NANOS_ATTR) as? Long)
    }

    private fun request(method: String = "GET", uri: String = "/api/main"): MockHttpServletRequest =
        MockHttpServletRequest(method, uri)
}
