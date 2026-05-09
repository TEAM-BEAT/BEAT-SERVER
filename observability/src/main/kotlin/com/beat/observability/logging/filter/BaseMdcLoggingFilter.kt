package com.beat.observability.logging.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

abstract class BaseMdcLoggingFilter : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID_HEADER = "X-Request-ID"
        const val X_FORWARDED_FOR_HEADER = "X-Forwarded-For"
        const val X_REAL_IP_HEADER = "X-Real-IP"

        const val TRACE_ID_KEY = "traceId"
        const val USER_ID_KEY = "userId"
        const val CLIENT_IP_KEY = "clientIp"
        const val REQUEST_INFO_KEY = "requestInfo"

        const val DEFAULT_GUEST_USER = "GUEST"

        private const val MAX_TRACE_ID_LENGTH = 128
        private val TRACE_ID_PATTERN = Regex("^[A-Za-z0-9._:-]+$")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = resolveTraceId(request)
        response.setHeader(TRACE_ID_HEADER, traceId)

        populateMdc(request, traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }

    private fun resolveTraceId(request: HttpServletRequest): String =
        request.getHeader(TRACE_ID_HEADER)
            ?.trim()
            ?.takeIf { it.length in 1..MAX_TRACE_ID_LENGTH }
            ?.takeIf { TRACE_ID_PATTERN.matches(it) }
            ?: generateTraceId()

    private fun generateTraceId(): String = UUID.randomUUID().toString().replace("-", "")

    private fun populateMdc(request: HttpServletRequest, traceId: String) {
        MDC.put(TRACE_ID_KEY, traceId)
        MDC.put(CLIENT_IP_KEY, extractClientIp(request))
        MDC.put(REQUEST_INFO_KEY, "${request.method} ${request.requestURI}")

        val userId = resolveUserId()
        MDC.put(USER_ID_KEY, userId?.takeIf { it.isNotBlank() } ?: DEFAULT_GUEST_USER)
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
