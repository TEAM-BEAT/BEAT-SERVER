package com.beat.observability.logging.filter

import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

abstract class BaseMdcLoggingFilter(
    private val tracer: Tracer? = null,
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
        private const val NOOP_TRACE_ID = "00000000000000000000000000000000"
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

    private fun resolveFallbackTraceId(request: HttpServletRequest): String =
        request.getHeader(TRACE_ID_HEADER)
            ?.trim()
            ?.takeIf { it.length in 1..MAX_TRACE_ID_LENGTH }
            ?.takeIf { TRACE_ID_PATTERN.matches(it) }
            ?: generateTraceId()

    private fun generateTraceId(): String = UUID.randomUUID().toString().replace("-", "")

    private fun populateMdc(request: HttpServletRequest): String {
        val fallbackTraceId = resolveFallbackTraceId(request)
        val otelContext = tracer?.currentSpan()?.context()
        val effectiveTraceId = if (otelContext != null &&
            otelContext.traceId().isNotBlank() &&
            otelContext.traceId() != NOOP_TRACE_ID
        ) {
            MDC.put(SPAN_ID_KEY, otelContext.spanId())
            otelContext.traceId()
        } else {
            fallbackTraceId
        }

        MDC.put(TRACE_ID_KEY, effectiveTraceId)
        MDC.put(CLIENT_IP_KEY, extractClientIp(request))
        MDC.put(REQUEST_INFO_KEY, "${request.method} ${request.requestURI}")

        val userId = resolveUserId()
        MDC.put(USER_ID_KEY, userId?.takeIf { it.isNotBlank() } ?: DEFAULT_GUEST_USER)

        return effectiveTraceId
    }

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
