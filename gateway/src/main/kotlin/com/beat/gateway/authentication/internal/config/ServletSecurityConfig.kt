package com.beat.gateway.authentication.internal.config

import com.beat.gateway.jwt.internal.config.JwtConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(
    JwtConfig::class,
    SecurityFilterConfig::class,
    WebMvcConfig::class,
)
class ServletSecurityConfig
