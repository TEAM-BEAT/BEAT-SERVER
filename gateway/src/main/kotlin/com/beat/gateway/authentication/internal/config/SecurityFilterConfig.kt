package com.beat.gateway.authentication.internal.config

import com.beat.gateway.authentication.internal.CustomAccessDeniedHandler
import com.beat.gateway.authentication.internal.CustomJwtAuthenticationEntryPoint
import com.beat.gateway.authentication.internal.JwtAuthenticationFilter
import com.beat.gateway.authentication.internal.SecurityMdcLoggingFilter
import com.beat.observability.tracing.TraceContextResolver
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(
    CustomAccessDeniedHandler::class,
    CustomJwtAuthenticationEntryPoint::class,
    JwtAuthenticationFilter::class,
)
class SecurityFilterConfig {

    @Bean(name = ["gatewaySecurityMdcLoggingFilter"])
    fun gatewaySecurityMdcLoggingFilter(
        traceContextResolver: TraceContextResolver,
        @Value("\${management.server.port}") managementPort: Int,
    ): SecurityMdcLoggingFilter = SecurityMdcLoggingFilter(traceContextResolver, managementPort)

    @Bean
    fun gatewaySecurityMdcLoggingFilterRegistration(
        @Qualifier("gatewaySecurityMdcLoggingFilter") filter: SecurityMdcLoggingFilter,
    ): FilterRegistrationBean<SecurityMdcLoggingFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }
}
