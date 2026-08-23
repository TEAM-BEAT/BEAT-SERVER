package com.beat.support.security.authentication.internal

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import com.beat.observability.tracing.TraceContextResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * SecurityContext의 principal을 MDC `userId`로 노출한다.
 * management port 요청은 access log/MDC 대상에서 제외한다.
 */
internal class SecurityMdcLoggingFilter(
    traceContextResolver: TraceContextResolver,
    private val managementPort: Int = NO_MANAGEMENT_PORT,
) : BaseMdcLoggingFilter(traceContextResolver) {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        managementPort > 0 && request.localPort == managementPort

    override fun resolveUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
            ?.takeIf { it !is AnonymousAuthenticationToken }
            ?: return null

        return (authentication.principal as? Long)?.toString()
            ?: authentication.name?.takeIf(String::isNotBlank)
    }

    companion object {
        private const val NO_MANAGEMENT_PORT = -1
    }
}
