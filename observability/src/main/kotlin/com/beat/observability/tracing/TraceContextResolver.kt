package com.beat.observability.tracing

import io.micrometer.tracing.Tracer

fun interface TraceContextResolver {
    fun resolve(): ResolvedTraceContext?

    data class ResolvedTraceContext(val traceId: String, val spanId: String)
}

object NoOpTraceContextResolver : TraceContextResolver {
    override fun resolve(): TraceContextResolver.ResolvedTraceContext? = null
}

class MicrometerTraceContextResolver(
    private val tracer: Tracer,
) : TraceContextResolver {

    override fun resolve(): TraceContextResolver.ResolvedTraceContext? {
        val context = tracer.currentSpan()?.context() ?: return null
        val traceId = context.traceId()
        if (traceId.isBlank() || traceId == NOOP_TRACE_ID) return null
        return TraceContextResolver.ResolvedTraceContext(traceId, context.spanId())
    }

    private companion object {
        const val NOOP_TRACE_ID = "00000000000000000000000000000000"
    }
}
