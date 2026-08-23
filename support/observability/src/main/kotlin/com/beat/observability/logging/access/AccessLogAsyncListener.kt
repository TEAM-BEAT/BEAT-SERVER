package com.beat.observability.logging.access

import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC

internal class AccessLogAsyncListener(
    private val emitter: AccessLogEmitter,
    private val mdcSnapshot: Map<String, String>,
) : AsyncListener {

    override fun onComplete(event: AsyncEvent) = emitOnce(event)

    override fun onTimeout(event: AsyncEvent) = emitOnce(event)

    override fun onError(event: AsyncEvent) {
        val req = event.suppliedRequest as? HttpServletRequest
        if (req != null && event.throwable != null && req.getAttribute(AccessLogEmitter.EXCEPTION_ATTR) == null) {
            req.setAttribute(AccessLogEmitter.EXCEPTION_ATTR, event.throwable)
        }
        emitOnce(event)
    }

    override fun onStartAsync(event: AsyncEvent) {
        event.asyncContext.addListener(this, event.suppliedRequest, event.suppliedResponse)
    }

    // Servlet spec: onTimeout/onError fire before the container's own onComplete call —
    // without this guard the request would emit two access log lines.
    private fun emitOnce(event: AsyncEvent) {
        val req = event.suppliedRequest as? HttpServletRequest ?: return
        val resp = event.suppliedResponse as? HttpServletResponse ?: return
        if (req.getAttribute(LOGGED_ATTR) != null) return
        req.setAttribute(LOGGED_ATTR, true)
        if (!emitter.shouldEmit(req)) return

        val previous = MDC.getCopyOfContextMap()
        try {
            MDC.setContextMap(mdcSnapshot)
            emitter.emit(req, resp)
        } finally {
            if (previous != null) MDC.setContextMap(previous) else MDC.clear()
        }
    }

    companion object {
        private const val LOGGED_ATTR = "beat.access.logged"
    }
}
