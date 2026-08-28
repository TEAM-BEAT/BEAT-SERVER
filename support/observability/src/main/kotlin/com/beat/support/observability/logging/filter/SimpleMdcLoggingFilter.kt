package com.beat.support.observability.logging.filter

import com.beat.support.observability.tracing.NoOpTraceContextResolver
import com.beat.support.observability.tracing.TraceContextResolver

class SimpleMdcLoggingFilter(resolver: TraceContextResolver = NoOpTraceContextResolver) :
    BaseMdcLoggingFilter(resolver) {
    override fun resolveUserId(): String? = null
}
