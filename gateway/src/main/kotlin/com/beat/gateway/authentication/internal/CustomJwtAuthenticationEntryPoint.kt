package com.beat.gateway.authentication.internal

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationEntryPoint : AuthenticationEntryPoint {

    private val log = LoggerFactory.getLogger(CustomJwtAuthenticationEntryPoint::class.java)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val path = request.requestURI
        val method = request.method
        log.warn("Unauthorized access attempt: Method: {}, Path: {}, Message: {}", method, path, authException.message)
        response.status = HttpServletResponse.SC_UNAUTHORIZED
    }
}
