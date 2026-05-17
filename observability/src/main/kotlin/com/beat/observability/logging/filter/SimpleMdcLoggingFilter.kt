package com.beat.observability.logging.filter

import io.micrometer.tracing.Tracer

class SimpleMdcLoggingFilter(tracer: Tracer? = null) : BaseMdcLoggingFilter(tracer) {
    override fun resolveUserId(): String? = null
}
