package com.beat.observability.logging.filter

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

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val effectiveTraceId = populateMdc(request)
        response.setHeader(TRACE_ID_HEADER, effectiveTraceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
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
        val forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER)
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",").first().trim()
        }

        val realIp = request.getHeader(X_REAL_IP_HEADER)
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }

        return request.remoteAddr
    }

    protected abstract fun resolveUserId(): String?
}
