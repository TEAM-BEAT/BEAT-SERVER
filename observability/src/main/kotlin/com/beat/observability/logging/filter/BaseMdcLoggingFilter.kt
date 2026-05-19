package com.beat.observability.logging.filter

import com.beat.observability.tracing.NoOpTraceContextResolver
import com.beat.observability.tracing.TraceContextResolver
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping
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
        const val STATUS_KEY = "status"
        const val ELAPSED_KEY = "elapsed"

        const val DEFAULT_GUEST_USER = "GUEST"
        const val DEFAULT_ROUTE_PATTERN = "NO_ROUTE"

        // Request attribute keys — internal lifecycle markers (not MDC fields).
        // The "beat.access." prefix avoids collision with Spring / Servlet attributes.
        const val START_NANOS_ATTR = "beat.access.startNanos"
        const val EXCEPTION_ATTR = "beat.access.exception"
        const val ACCESS_LOGGED_ATTR = "beat.access.logged"

        // Kill-switch: set BEAT_ACCESS_LOG_ENABLED=false to suppress access log emission
        // without redeployment (see docs/observability/LOGGING_GUIDE.md §8).
        const val ACCESS_LOG_ENABLED_ENV = "BEAT_ACCESS_LOG_ENABLED"

        // Logger name kept in sync with log4j2-spring.xml's <Logger name="..."/>.
        // Isolated from SentryAppender so access log lines never become Sentry events/breadcrumbs.
        const val ACCESS_LOGGER_NAME = "com.beat.observability.logging.access"

        // SERVER_ERROR_THRESHOLD: 5xx status codes are emitted at ERROR level so the business
        // logger pipeline (com.beat → SentryAppender) does not duplicate-capture them.
        // The access logger itself is Sentry-isolated; ERROR level here is purely for Loki filtering.
        private const val SERVER_ERROR_THRESHOLD = 500
        private const val NANOS_PER_MILLI = 1_000_000L

        private const val MAX_TRACE_ID_LENGTH = 128
        private val TRACE_ID_PATTERN = Regex("^[A-Za-z0-9._:-]+$")
        private val SKIP_HTTP_METHODS: Set<String> = setOf(HttpMethod.OPTIONS.name())

        // Actuator health paths on the same port (local profile) — high-frequency infra probes
        // with no business value in access log. In prod/dev the management server runs on a
        // separate port so these paths never reach this filter in production anyway.
        private val SKIP_PATHS: Set<String> = setOf(
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
        )

        val ACCESS_LOG_ENABLED: Boolean =
            System.getenv(ACCESS_LOG_ENABLED_ENV)?.toBooleanStrictOrNull() ?: true

        internal val accessLog: Logger = LoggerFactory.getLogger(ACCESS_LOGGER_NAME)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val isInitialRequest = request.dispatcherType == DispatcherType.REQUEST

        if (isInitialRequest) {
            val effectiveTraceId = populateMdc(request)
            response.setHeader(TRACE_ID_HEADER, effectiveTraceId)
            request.setAttribute(START_NANOS_ATTR, System.nanoTime())
        }

        try {
            filterChain.doFilter(request, response)
        } catch (ex: Throwable) {
            // @ControllerAdvice swallows the throwable before it reaches this filter; store for access log.
            request.setAttribute(EXCEPTION_ATTR, ex)
            throw ex
        } finally {
            try {
                if (request.isAsyncStarted) {
                    registerAsyncAccessLog(request, response, isInitialRequest)
                } else if (isInitialRequest && shouldEmitAccessLog(request)) {
                    // JWT filter has already run — resolve real userId now.
                    refreshUserIdInMdc()
                    emitAccessLog(request, response)
                }
                // ASYNC re-dispatch path: AsyncLogListener.onComplete already emitted the log.
            } finally {
                MDC.clear()
            }
        }
    }

    private fun registerAsyncAccessLog(
        request: HttpServletRequest,
        response: HttpServletResponse,
        isInitialRequest: Boolean,
    ) {
        // Capture MDC for the async worker thread (ThreadLocals are not inherited).
        val snapshot = MDC.getCopyOfContextMap() ?: emptyMap()
        try {
            request.asyncContext.addListener(AccessLogAsyncListener(snapshot), request, response)
        } catch (_: IllegalStateException) {
            // TOCTOU: async completed between isAsyncStarted check and addListener.
            // Fall back to synchronous emit so the access log is never lost.
            if (isInitialRequest && shouldEmitAccessLog(request)) {
                MDC.setContextMap(snapshot)
                refreshUserIdInMdc()
                emitAccessLog(request, response)
            }
        }
    }

    private fun shouldEmitAccessLog(request: HttpServletRequest): Boolean =
        ACCESS_LOG_ENABLED &&
            request.method !in SKIP_HTTP_METHODS &&
            request.requestURI !in SKIP_PATHS

    internal fun refreshUserIdInMdc() {
        MDC.put(USER_ID_KEY, resolveUserId()?.takeIf { it.isNotBlank() } ?: DEFAULT_GUEST_USER)
    }

    internal fun emitAccessLog(request: HttpServletRequest, response: HttpServletResponse) {
        val startNanos = request.getAttribute(START_NANOS_ATTR) as? Long ?: System.nanoTime()
        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
        val status = response.status

        if (MDC.get(ROUTE_PATTERN_KEY).isNullOrBlank()) {
            MDC.put(ROUTE_PATTERN_KEY, resolveRoutePatternFallback(request))
        }
        MDC.put(STATUS_KEY, status.toString())
        MDC.put(ELAPSED_KEY, elapsedMs.toString())

        // Message is plain "HTTP <status>" — structured fields live in MDC and the JSON template
        // expands them to top-level keys (http_status, elapsed_ms, route, trace_id, ...).
        val message = "HTTP $status"
        val ex = request.getAttribute(EXCEPTION_ATTR) as? Throwable
        when {
            status >= SERVER_ERROR_THRESHOLD && ex != null -> accessLog.error(message, ex)
            status >= SERVER_ERROR_THRESHOLD -> accessLog.error(message)
            else -> accessLog.info(message)
        }
    }

    private fun resolveRoutePatternFallback(request: HttpServletRequest): String =
        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
            ?.let { "${request.method} $it" }
            ?: DEFAULT_ROUTE_PATTERN

    private inner class AccessLogAsyncListener(
        private val mdcSnapshot: Map<String, String>,
    ) : AsyncListener {

        // Servlet async lifecycle: onTimeout/onError fire BEFORE the container calls onComplete.
        // Without a guard, calling onComplete from onTimeout/onError and then receiving the
        // container's own onComplete call emits two access log lines for the same request.
        private fun emitOnce(event: AsyncEvent) {
            val req = event.suppliedRequest as? HttpServletRequest ?: return
            val resp = event.suppliedResponse as? HttpServletResponse ?: return
            if (req.getAttribute(ACCESS_LOGGED_ATTR) != null) return
            req.setAttribute(ACCESS_LOGGED_ATTR, true)
            if (!shouldEmitAccessLog(req)) return

            val prev = MDC.getCopyOfContextMap()
            try {
                MDC.setContextMap(mdcSnapshot)
                refreshUserIdInMdc()
                emitAccessLog(req, resp)
            } finally {
                if (prev != null) MDC.setContextMap(prev) else MDC.clear()
            }
        }

        override fun onComplete(event: AsyncEvent) = emitOnce(event)

        override fun onTimeout(event: AsyncEvent) = emitOnce(event)

        override fun onError(event: AsyncEvent) {
            val req = event.suppliedRequest as? HttpServletRequest
            if (req != null && event.throwable != null && req.getAttribute(EXCEPTION_ATTR) == null) {
                req.setAttribute(EXCEPTION_ATTR, event.throwable)
            }
            emitOnce(event)
        }

        // Re-register on each new async cycle so chained async calls remain covered.
        override fun onStartAsync(event: AsyncEvent) {
            event.asyncContext.addListener(this, event.suppliedRequest, event.suppliedResponse)
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
