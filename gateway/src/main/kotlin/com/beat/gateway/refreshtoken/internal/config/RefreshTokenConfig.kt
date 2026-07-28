package com.beat.gateway.refreshtoken.internal.config

import com.beat.gateway.refreshtoken.internal.RefreshTokenService
import com.beat.gateway.shared.internal.config.RedisConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(RedisConfig::class, RefreshTokenService::class)
class RefreshTokenConfig
