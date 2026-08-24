package com.beat.support.security.authentication.internal.config

import com.beat.support.security.jwt.internal.config.JwtConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(
    JwtConfig::class,
    SecurityFilterConfig::class,
    WebMvcConfig::class,
)
internal class ServletSecurityConfig
