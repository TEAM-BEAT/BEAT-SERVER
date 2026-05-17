package com.beat.observability.logging.filter

import com.beat.observability.tracing.NoOpTraceContextResolver
import com.beat.observability.tracing.TraceContextResolver

class SimpleMdcLoggingFilter(
    resolver: TraceContextResolver = NoOpTraceContextResolver,
) : BaseMdcLoggingFilter(resolver) {
    override fun resolveUserId(): String? = null
}
