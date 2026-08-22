package com.beat.observability.logging.exception

import com.beat.observability.logging.access.AccessLogEmitter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.springframework.core.Ordered
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ExceptionCaptureResolverTest : FunSpec() {

    private val resolver = ExceptionCaptureResolver()

    init {
        test("stores the exception on the request attribute used by AccessLogEmitter") {
            val request = MockHttpServletRequest("POST", "/api/bookings")
            val cause = RuntimeException("db timeout")

            val result = resolver.resolveException(request, MockHttpServletResponse(), null, cause)

            request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR) shouldBeSameInstanceAs cause
            result.shouldBeNull()
        }

        test("runs before all other HandlerExceptionResolvers") {
            // Required to capture the exception before @ControllerAdvice resolves and swallows it.
            resolver.order shouldBe Ordered.HIGHEST_PRECEDENCE
        }
    }
}
