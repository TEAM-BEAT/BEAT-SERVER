package com.beat.gateway.refreshtoken.internal.config

import com.beat.contracts.auth.refreshtoken.RefreshTokenPort
import org.springframework.context.annotation.Configuration

/**
 * 기존 gateway bootstrap API를 유지하면서 composition root가
 * RefreshTokenPort adapter를 제공했는지 fail-fast로 검증한다.
 */
@Configuration(proxyBeanMethods = false)
class RefreshTokenConfig(
    @Suppress("UNUSED_PARAMETER")
    refreshTokenPort: RefreshTokenPort,
)
