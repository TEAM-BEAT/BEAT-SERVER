package com.beat.support.security.authentication.internal.config

import com.beat.support.security.authentication.internal.CustomAccessDeniedHandler
import com.beat.support.security.authentication.internal.CustomJwtAuthenticationEntryPoint
import com.beat.support.security.authentication.internal.JwtAuthenticationFilter
import com.beat.support.security.authentication.internal.SecurityMdcLoggingFilter
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
internal class SecurityFilterConfig {

    @Bean(name = ["gatewaySecurityMdcLoggingFilter"])
    fun gatewaySecurityMdcLoggingFilter(
        traceContextResolver: TraceContextResolver,
        @Value("\${management.server.port}") managementPort: Int,
        @Value("\${management.endpoints.web.base-path:/actuator}") actuatorBasePath: String,
    ): SecurityMdcLoggingFilter = SecurityMdcLoggingFilter(traceContextResolver, managementPort, actuatorBasePath)

    @Bean
    fun gatewaySecurityMdcLoggingFilterRegistration(
        @Qualifier("gatewaySecurityMdcLoggingFilter") filter: SecurityMdcLoggingFilter,
    ): FilterRegistrationBean<SecurityMdcLoggingFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }
}
