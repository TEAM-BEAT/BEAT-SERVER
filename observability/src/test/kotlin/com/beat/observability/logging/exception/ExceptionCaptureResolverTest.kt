package com.beat.observability.logging.exception

import com.beat.observability.logging.access.AccessLogEmitter
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ExceptionCaptureResolverTest {

    private val resolver = ExceptionCaptureResolver()

    @Test
    fun `stores the exception on the request attribute used by AccessLogEmitter`() {
        val request = MockHttpServletRequest("POST", "/api/bookings")
        val cause = RuntimeException("db timeout")

        val result = resolver.resolveException(request, MockHttpServletResponse(), null, cause)

        assertSame(cause, request.getAttribute(AccessLogEmitter.EXCEPTION_ATTR))
        assertNull(result, "resolver must return null so downstream resolvers still handle the response")
    }

    @Test
    fun `runs before all other HandlerExceptionResolvers`() {
        // Required to capture the exception before @ControllerAdvice resolves and swallows it.
        assert(resolver.order == Ordered.HIGHEST_PRECEDENCE)
    }
}
