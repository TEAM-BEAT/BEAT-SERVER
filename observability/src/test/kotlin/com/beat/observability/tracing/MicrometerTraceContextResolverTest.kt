package com.beat.observability.tracing

import io.micrometer.tracing.Span
import io.micrometer.tracing.TraceContext
import io.micrometer.tracing.Tracer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MicrometerTraceContextResolverTest {

    @Test
    fun `returns null when tracer has no active span`() {
        val tracer = mock(Tracer::class.java)
        `when`(tracer.currentSpan()).thenReturn(null)

        val resolved = MicrometerTraceContextResolver(tracer).resolve()

        assertNull(resolved)
    }

    @Test
    fun `returns null when traceId is blank`() {
        val tracer = mockTracer(traceId = "   ", spanId = "abc")

        val resolved = MicrometerTraceContextResolver(tracer).resolve()

        assertNull(resolved)
    }

    @Test
    fun `returns null when traceId is the OTel noop id`() {
        val tracer = mockTracer(
            traceId = "00000000000000000000000000000000",
            spanId = "0000000000000000",
        )

        val resolved = MicrometerTraceContextResolver(tracer).resolve()

        assertNull(resolved)
    }

    @Test
    fun `returns resolved context for a real active span`() {
        val tracer = mockTracer(
            traceId = "abcdef0123456789abcdef0123456789",
            spanId = "fedcba9876543210",
        )

        val resolved = MicrometerTraceContextResolver(tracer).resolve()

        assertEquals("abcdef0123456789abcdef0123456789", resolved?.traceId)
        assertEquals("fedcba9876543210", resolved?.spanId)
    }

    private fun mockTracer(traceId: String, spanId: String): Tracer {
        val context = mock(TraceContext::class.java)
        `when`(context.traceId()).thenReturn(traceId)
        `when`(context.spanId()).thenReturn(spanId)
        val span = mock(Span::class.java)
        `when`(span.context()).thenReturn(context)
        val tracer = mock(Tracer::class.java)
        `when`(tracer.currentSpan()).thenReturn(span)
        return tracer
    }
}
