package com.beat.gateway.authentication.internal

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler : AccessDeniedHandler {

    private val log = LoggerFactory.getLogger(CustomAccessDeniedHandler::class.java)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        val path = request.requestURI
        val method = request.method
        log.warn("Access Denied: Method: {}, Path: {}, Message: {}", method, path, accessDeniedException.message)
        response.status = HttpServletResponse.SC_FORBIDDEN
    }
}
