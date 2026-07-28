package com.beat.gateway

import com.beat.gateway.authentication.internal.config.ServletSecurityConfig
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(ServletSecurityConfig::class)
annotation class EnableGatewayServletSecurity
