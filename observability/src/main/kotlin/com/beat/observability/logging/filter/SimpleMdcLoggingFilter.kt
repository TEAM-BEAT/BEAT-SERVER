package com.beat.observability.logging.filter

class SimpleMdcLoggingFilter : BaseMdcLoggingFilter() {
    override fun resolveUserId(): String? = null
}
