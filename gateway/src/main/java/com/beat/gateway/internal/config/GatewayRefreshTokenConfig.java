package com.beat.gateway.internal.config;

import com.beat.gateway.jwt.internal.RefreshTokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({GatewayRedisConfig.class, RefreshTokenService.class})
public class GatewayRefreshTokenConfig {
}
