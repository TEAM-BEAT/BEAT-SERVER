package com.beat.observability.logging.access

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AccessLogAsyncListenerTest : FunSpec() {

    init {
        afterTest { MDC.clear() }

        test("onComplete은 request lifecycle 동안 access log를 정확히 한 번 emit한다") {
            val emitter = RecordingEmitter()
            val request = request()
            val response = MockHttpServletResponse().apply { status = 200 }
            val listener = AccessLogAsyncListener(emitter, mdcSnapshot = emptyMap())

            listener.onComplete(event(request, response))

            emitter.emitCount shouldBe 1
            (request.getAttribute("beat.access.logged") as? Boolean) shouldBe true
        }

        test("onTimeout 이후 이어지는 onComplete은 중복으로 emit하지 않는다") {
            val emitter = RecordingEmitter()
            val request = request()
            val response = MockHttpServletResponse().apply { status = 504 }
            val listener = AccessLogAsyncListener(emitter, mdcSnapshot = emptyMap())

            listener.onTimeout(event(request, response))
            listener.onComplete(event(request, response))

            emitter.emitCount shouldBe 1
        }

        test("아직 capture된 exception이 없으면 onError는 event의 throwable을 request attribute에 저장한다") {
            val cause = IllegalStateException("downstream broke")
            val request = request()
            val response = MockHttpServletResponse().apply { status = 500 }
            val listener = AccessLogAsyncListener(RecordingEmitter(), mdcSnapshot = emptyMap())

            listener.onError(event(request, response, throwable = cause))

            request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR) shouldBeSameInstanceAs cause
        }

        test("onError는 ExceptionCaptureResolver가 이미 capture한 exception을 유지한다") {
            val captured = RuntimeException("primary")
            val asyncCause = IllegalStateException("secondary")
            val request = request().apply { setAttribute(AccessLogEmitter.EXCEPTION_ATTR, captured) }
            val response = MockHttpServletResponse().apply { status = 500 }
            val listener = AccessLogAsyncListener(RecordingEmitter(), mdcSnapshot = emptyMap())

            listener.onError(event(request, response, throwable = asyncCause))

            request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR) shouldBeSameInstanceAs captured
        }

        test("onStartAsync는 자기 자신을 새 async context에 등록해 이어지는 async cycle도 커버한다") {
            val newAsyncContext = RecordingAsyncContext()
            val listener = AccessLogAsyncListener(RecordingEmitter(), mdcSnapshot = emptyMap())

            listener.onStartAsync(AsyncEvent(newAsyncContext, request(), MockHttpServletResponse()))

            newAsyncContext.addedListeners.size shouldBe 1
            newAsyncContext.addedListeners[0] shouldBeSameInstanceAs listener
        }

        test("일반 경로는 emit되고 logged flag가 설정된다") {
            val emitter = RecordingEmitter()
            val request = request(uri = "/api/main")
            val response = MockHttpServletResponse()
            val listener = AccessLogAsyncListener(emitter, mdcSnapshot = emptyMap())

            listener.onComplete(event(request, response))

            emitter.emitCount shouldBe 1
            (request.getAttribute("beat.access.logged") as? Boolean) shouldBe true
        }

        test("emit 중에는 MDC snapshot을 적용하고 emit이 끝나면 복원한다") {
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

            capturedMdcDuringEmit[BaseMdcLoggingFilter.TRACE_ID_KEY] shouldBe "captured-trace"
            capturedMdcDuringEmit[BaseMdcLoggingFilter.USER_ID_KEY] shouldBe "42"
            MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldBeNull()
        }

        test("worker thread의 기존 MDC는 emit 과정에서 보존한다") {
            MDC.put("unrelated", "preserved")
            val listener = AccessLogAsyncListener(
                RecordingEmitter(),
                mdcSnapshot = mapOf(BaseMdcLoggingFilter.TRACE_ID_KEY to "should-be-overlaid-then-restored"),
            )

            listener.onComplete(event(request(), MockHttpServletResponse()))

            MDC.get("unrelated") shouldBe "preserved"
            MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY).shouldBeNull()
        }
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
