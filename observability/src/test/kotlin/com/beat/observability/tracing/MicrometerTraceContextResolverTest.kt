package com.beat.observability.tracing

import io.micrometer.tracing.Span
import io.micrometer.tracing.TraceContext
import io.micrometer.tracing.Tracer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MicrometerTraceContextResolverTest : FunSpec() {

    init {
        test("tracer에 active span이 없으면 null을 반환한다") {
            val tracer = mock(Tracer::class.java)
            `when`(tracer.currentSpan()).thenReturn(null)

            val resolved = MicrometerTraceContextResolver(tracer).resolve()

            resolved.shouldBeNull()
        }

        test("traceId가 blank이면 null을 반환한다") {
            val tracer = mockTracer(traceId = "   ", spanId = "abc")

            val resolved = MicrometerTraceContextResolver(tracer).resolve()

            resolved.shouldBeNull()
        }

        test("traceId가 OTel noop id이면 null을 반환한다") {
            val tracer = mockTracer(
                traceId = "00000000000000000000000000000000",
                spanId = "0000000000000000",
            )

            val resolved = MicrometerTraceContextResolver(tracer).resolve()

            resolved.shouldBeNull()
        }

        test("실제 active span이 있으면 resolved context를 반환한다") {
            val tracer = mockTracer(
                traceId = "abcdef0123456789abcdef0123456789",
                spanId = "fedcba9876543210",
            )

            val resolved = MicrometerTraceContextResolver(tracer).resolve()

            resolved?.traceId shouldBe "abcdef0123456789abcdef0123456789"
            resolved?.spanId shouldBe "fedcba9876543210"
        }
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
