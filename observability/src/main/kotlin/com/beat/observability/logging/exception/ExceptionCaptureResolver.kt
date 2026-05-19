package com.beat.observability.logging.exception

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

class ExceptionCaptureResolver : HandlerExceptionResolver, Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception,
    ): ModelAndView? {
        request.setAttribute(BaseMdcLoggingFilter.EXCEPTION_ATTR, ex)
        return null
    }
}
