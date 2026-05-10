package com.beat.observability.logging.interceptor

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

class RoutePatternMdcInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, resolveRoutePattern(request))
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        MDC.remove(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY)
    }

    private fun resolveRoutePattern(request: HttpServletRequest): String {
        val bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: return BaseMdcLoggingFilter.DEFAULT_ROUTE_PATTERN

        return "${request.method} $bestMatchingPattern"
    }
}
