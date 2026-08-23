package com.beat.observability.logging

import com.beat.observability.logging.exception.ExceptionCaptureResolver
import com.beat.observability.logging.interceptor.RoutePatternMdcInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class LoggingConfig {

    @Bean
    fun mdcTaskDecorator(): TaskDecorator = MdcTaskDecorator()

    @Bean
    fun routePatternMdcInterceptor(): RoutePatternMdcInterceptor = RoutePatternMdcInterceptor()

    @Bean
    fun routePatternMdcWebMvcConfigurer(routePatternMdcInterceptor: RoutePatternMdcInterceptor): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(routePatternMdcInterceptor)
            }
        }

    @Bean
    fun exceptionCaptureResolver(): ExceptionCaptureResolver = ExceptionCaptureResolver()
}
