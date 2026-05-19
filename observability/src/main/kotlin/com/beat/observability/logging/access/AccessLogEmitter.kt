package com.beat.observability.logging.access

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.HandlerMapping

open class AccessLogEmitter {

    fun markStart(request: HttpServletRequest) {
        request.setAttribute(START_NANOS_ATTR, System.nanoTime())
    }

    fun shouldEmit(request: HttpServletRequest): Boolean =
        ACCESS_LOG_ENABLED &&
            request.method !in SKIP_HTTP_METHODS &&
            SKIP_PATH_PREFIXES.none(request.requestURI::startsWith)

    open fun emit(request: HttpServletRequest, response: HttpServletResponse) {
        val startNanos = request.getAttribute(START_NANOS_ATTR) as? Long ?: System.nanoTime()
        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
        val status = response.status

        if (MDC.get(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY).isNullOrBlank()) {
            MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, resolveRoutePatternFallback(request))
        }
        MDC.put(STATUS_KEY, status.toString())
        MDC.put(ELAPSED_KEY, elapsedMs.toString())

        val message = "HTTP $status ${elapsedMs}ms"
        val ex = request.getAttribute(EXCEPTION_ATTR) as? Throwable
        when {
            status >= SERVER_ERROR_THRESHOLD && ex != null -> logger.error(message, ex)
            status >= SERVER_ERROR_THRESHOLD -> logger.error(message)
            else -> logger.info(message)
        }
    }

    private fun resolveRoutePatternFallback(request: HttpServletRequest): String =
        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
            ?.let { "${request.method} $it" }
            ?: BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN

    companion object {
        const val STATUS_KEY = "status"
        const val ELAPSED_KEY = "elapsed"
        const val LOGGER_NAME = "com.beat.observability.logging.access"

        // @ControllerAdvice swallows the exception before BaseMdcLoggingFilter's catch can see it;
        // ExceptionCaptureResolver (HIGHEST_PRECEDENCE HandlerExceptionResolver) stores it here.
        const val EXCEPTION_ATTR = "beat.access.exception"
        const val START_NANOS_ATTR = "beat.access.startNanos"

        val ACCESS_LOG_ENABLED: Boolean =
            System.getenv("BEAT_ACCESS_LOG_ENABLED")?.toBooleanStrictOrNull() ?: true

        private const val SERVER_ERROR_THRESHOLD = 500
        private const val NANOS_PER_MILLI = 1_000_000L

        private val SKIP_HTTP_METHODS = setOf(HttpMethod.OPTIONS.name())

        // Defensive against local-profile actuator on the same port. prod/dev expose
        // actuator on a separate management port, so paths never reach this filter there.
        private val SKIP_PATH_PREFIXES = setOf("/actuator/health")

        private val logger: Logger = LoggerFactory.getLogger(LOGGER_NAME)
    }
}
