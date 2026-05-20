package com.beat.observability.logging.filter

import com.beat.observability.logging.access.AccessLogAsyncListener
import com.beat.observability.logging.access.AccessLogEmitter
import com.beat.observability.tracing.NoOpTraceContextResolver
import com.beat.observability.tracing.TraceContextResolver
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

abstract class BaseMdcLoggingFilter(
    private val traceContextResolver: TraceContextResolver = NoOpTraceContextResolver,
) : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID_HEADER = "X-Request-ID"
        const val X_FORWARDED_FOR_HEADER = "X-Forwarded-For"
        const val X_REAL_IP_HEADER = "X-Real-IP"

        const val TRACE_ID_KEY = "traceId"
        const val SPAN_ID_KEY = "spanId"
        const val USER_ID_KEY = "userId"
        const val CLIENT_IP_KEY = "clientIp"
        const val REQUEST_INFO_KEY = "requestInfo"
        const val ROUTE_PATTERN_KEY = "routePattern"

        const val DEFAULT_GUEST_USER = "GUEST"
        const val DEFAULT_ROUTE_PATTERN = "NO_ROUTE"

        private const val MAX_TRACE_ID_LENGTH = 128
        private val TRACE_ID_PATTERN = Regex("^[A-Za-z0-9._:-]+$")
    }

    private val accessLog = AccessLogEmitter()

    // OncePerRequestFilter.shouldNotFilterAsyncDispatch() defaults to true → this filter runs
    // only on DispatcherType.REQUEST. ASYNC re-dispatch never invokes us; AsyncListener does.

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val effectiveTraceId = populateMdc(request)
        response.setHeader(TRACE_ID_HEADER, effectiveTraceId)
        accessLog.markStart(request)

        try {
            filterChain.doFilter(request, response)
        } catch (ex: Throwable) {
            request.setAttribute(AccessLogEmitter.EXCEPTION_ATTR, ex)
            throw ex
        } finally {
            try {
                // Refresh before snapshot/emit: JWT filter has now run on this thread.
                // The async worker thread that runs onComplete cannot see SecurityContextHolder.
                refreshUserIdInMdc()
                emitOrDeferAccessLog(request, response)
            } finally {
                MDC.clear()
            }
        }
    }

    private fun emitOrDeferAccessLog(request: HttpServletRequest, response: HttpServletResponse) {
        if (request.isAsyncStarted) {
            val snapshot = MDC.getCopyOfContextMap() ?: emptyMap()
            try {
                request.asyncContext.addListener(
                    AccessLogAsyncListener(accessLog, snapshot),
                    request,
                    response,
                )
            } catch (_: IllegalStateException) {
                // TOCTOU: async completed between isAsyncStarted check and addListener.
                if (accessLog.shouldEmit(request)) {
                    MDC.setContextMap(snapshot)
                    accessLog.emit(request, response)
                }
            }
            return
        }
        if (accessLog.shouldEmit(request)) {
            accessLog.emit(request, response)
        }
    }

    internal fun refreshUserIdInMdc() {
        MDC.put(USER_ID_KEY, resolveUserId()?.takeIf { it.isNotBlank() } ?: DEFAULT_GUEST_USER)
    }

    private fun populateMdc(request: HttpServletRequest): String {
        val traceId = applyTraceContext(request)
        MDC.put(CLIENT_IP_KEY, extractClientIp(request))
        MDC.put(REQUEST_INFO_KEY, "${request.method} ${request.requestURI}")
        MDC.put(USER_ID_KEY, resolveUserId()?.takeIf { it.isNotBlank() } ?: DEFAULT_GUEST_USER)
        return traceId
    }

    private fun applyTraceContext(request: HttpServletRequest): String {
        val resolved = traceContextResolver.resolve()
        val traceId = if (resolved != null) {
            MDC.put(SPAN_ID_KEY, resolved.spanId)
            resolved.traceId
        } else {
            resolveFallbackTraceId(request)
        }
        MDC.put(TRACE_ID_KEY, traceId)
        return traceId
    }

    private fun resolveFallbackTraceId(request: HttpServletRequest): String =
        request.getHeader(TRACE_ID_HEADER)
            ?.trim()
            ?.takeIf { it.length in 1..MAX_TRACE_ID_LENGTH }
            ?.takeIf { TRACE_ID_PATTERN.matches(it) }
            ?: generateTraceId()

    private fun generateTraceId(): String = UUID.randomUUID().toString().replace("-", "")

    private fun extractClientIp(request: HttpServletRequest): String {
        request.getHeader(X_FORWARDED_FOR_HEADER)?.takeIf { it.isNotBlank() }
            ?.let { return it.split(",").first().trim() }
        request.getHeader(X_REAL_IP_HEADER)?.takeIf { it.isNotBlank() }
            ?.let { return it.trim() }
        return request.remoteAddr
    }

    protected abstract fun resolveUserId(): String?
}
