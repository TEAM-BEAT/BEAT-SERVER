package com.beat.observability.logging.access

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AccessLogAsyncListenerTest {

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `onComplete emits access log exactly once for the request lifecycle`() {
        val emitter = RecordingEmitter()
        val request = request()
        val response = MockHttpServletResponse().apply { status = 200 }
        val listener = AccessLogAsyncListener(emitter, mdcSnapshot = emptyMap())

        listener.onComplete(event(request, response))

        assertEquals(1, emitter.emitCount)
        assertTrue((request.getAttribute("beat.access.logged") as? Boolean) == true)
    }

    @Test
    fun `subsequent onComplete after onTimeout does not double-emit`() {
        val emitter = RecordingEmitter()
        val request = request()
        val response = MockHttpServletResponse().apply { status = 504 }
        val listener = AccessLogAsyncListener(emitter, mdcSnapshot = emptyMap())

        listener.onTimeout(event(request, response))
        listener.onComplete(event(request, response))

        assertEquals(1, emitter.emitCount, "Container's redundant onComplete after onTimeout must be guarded")
    }

    @Test
    fun `onError stores event throwable on request attribute when none was captured yet`() {
        val cause = IllegalStateException("downstream broke")
        val request = request()
        val response = MockHttpServletResponse().apply { status = 500 }
        val listener = AccessLogAsyncListener(RecordingEmitter(), mdcSnapshot = emptyMap())

        listener.onError(event(request, response, throwable = cause))

        assertSame(cause, request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR))
    }

    @Test
    fun `onError preserves exception already captured by ExceptionCaptureResolver`() {
        val captured = RuntimeException("primary")
        val asyncCause = IllegalStateException("secondary")
        val request = request().apply { setAttribute(AccessLogEmitter.EXCEPTION_ATTR, captured) }
        val response = MockHttpServletResponse().apply { status = 500 }
        val listener = AccessLogAsyncListener(RecordingEmitter(), mdcSnapshot = emptyMap())

        listener.onError(event(request, response, throwable = asyncCause))

        assertSame(captured, request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR))
    }

    @Test
    fun `onStartAsync registers itself on the new async context so chained async cycles are covered`() {
        val newAsyncContext = RecordingAsyncContext()
        val listener = AccessLogAsyncListener(RecordingEmitter(), mdcSnapshot = emptyMap())

        listener.onStartAsync(AsyncEvent(newAsyncContext, request(), MockHttpServletResponse()))

        assertEquals(1, newAsyncContext.addedListeners.size)
        assertSame(listener, newAsyncContext.addedListeners[0])
    }

    @Test
    fun `skip paths bypass emission but still set the logged flag`() {
        val emitter = RecordingEmitter()
        val request = request(uri = "/actuator/health")
        val response = MockHttpServletResponse()
        val listener = AccessLogAsyncListener(emitter, mdcSnapshot = emptyMap())

        listener.onComplete(event(request, response))

        assertEquals(0, emitter.emitCount)
        assertTrue((request.getAttribute("beat.access.logged") as? Boolean) == true)
    }

    @Test
    fun `MDC snapshot is installed during emit and restored afterwards`() {
        val capturedMdcDuringEmit = mutableMapOf<String, String>()
        val emitter = object : AccessLogEmitter() {
            override fun emit(request: HttpServletRequest, response: HttpServletResponse) {
                MDC.getCopyOfContextMap()?.let(capturedMdcDuringEmit::putAll)
            }
        }
        val snapshot = mapOf(
            BaseMdcLoggingFilter.TRACE_ID_KEY to "captured-trace",
            BaseMdcLoggingFilter.USER_ID_KEY to "42",
        )
        val listener = AccessLogAsyncListener(emitter, mdcSnapshot = snapshot)

        listener.onComplete(event(request(), MockHttpServletResponse()))

        assertEquals("captured-trace", capturedMdcDuringEmit[BaseMdcLoggingFilter.TRACE_ID_KEY])
        assertEquals("42", capturedMdcDuringEmit[BaseMdcLoggingFilter.USER_ID_KEY])
        assertNull(MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY), "snapshot must not leak into worker thread MDC")
    }

    @Test
    fun `previous MDC of the worker thread is preserved across emit`() {
        MDC.put("unrelated", "preserved")
        val listener = AccessLogAsyncListener(
            RecordingEmitter(),
            mdcSnapshot = mapOf(BaseMdcLoggingFilter.TRACE_ID_KEY to "should-be-overlaid-then-restored"),
        )

        listener.onComplete(event(request(), MockHttpServletResponse()))

        assertEquals("preserved", MDC.get("unrelated"))
        assertNull(MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
    }

    private open class RecordingEmitter : AccessLogEmitter() {
        var emitCount: Int = 0
            private set

        override fun emit(request: HttpServletRequest, response: HttpServletResponse) {
            emitCount++
        }
    }

    private fun request(method: String = "GET", uri: String = "/api/main"): MockHttpServletRequest =
        MockHttpServletRequest(method, uri)

    private fun event(
        request: MockHttpServletRequest,
        response: MockHttpServletResponse,
        throwable: Throwable? = null,
    ): AsyncEvent = AsyncEvent(RecordingAsyncContext(), request, response, throwable)

    private class RecordingAsyncContext : AsyncContext {
        val addedListeners = mutableListOf<jakarta.servlet.AsyncListener>()

        override fun addListener(listener: jakarta.servlet.AsyncListener) {
            addedListeners.add(listener)
        }

        override fun addListener(
            listener: jakarta.servlet.AsyncListener,
            servletRequest: jakarta.servlet.ServletRequest,
            servletResponse: jakarta.servlet.ServletResponse,
        ) {
            addedListeners.add(listener)
        }

        override fun getRequest(): jakarta.servlet.ServletRequest = throw UnsupportedOperationException()
        override fun getResponse(): jakarta.servlet.ServletResponse = throw UnsupportedOperationException()
        override fun hasOriginalRequestAndResponse(): Boolean = true
        override fun dispatch() {}
        override fun dispatch(path: String) {}
        override fun dispatch(context: jakarta.servlet.ServletContext, path: String) {}
        override fun complete() {}
        override fun start(run: Runnable) {}
        override fun <T : jakarta.servlet.AsyncListener> createListener(clazz: Class<T>): T = throw UnsupportedOperationException()
        override fun setTimeout(timeout: Long) {}
        override fun getTimeout(): Long = 0
    }
}
