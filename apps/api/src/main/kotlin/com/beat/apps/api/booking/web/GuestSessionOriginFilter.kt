package com.beat.apps.api.booking.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class GuestSessionOriginFilter(
    @Value("\${cors.allowed-origins}") allowedOrigins: Array<String>,
    private val accessDeniedHandler: AccessDeniedHandler,
) : OncePerRequestFilter() {
    private val allowedOrigins = allowedOrigins.toSet()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "PATCH" ||
            request.requestURI !in GUEST_BOOKING_MUTATION_PATHS ||
            request.cookies?.none { it.name == GUEST_SESSION_COOKIE_NAME } != false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val origin = request.getHeader(ORIGIN_HEADER)
        if (origin !in allowedOrigins) {
            accessDeniedHandler.handle(
                request,
                response,
                AccessDeniedException("Invalid guest request origin"),
            )
            return
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val ORIGIN_HEADER = "Origin"
    }
}
