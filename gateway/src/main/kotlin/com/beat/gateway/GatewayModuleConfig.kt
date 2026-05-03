package com.beat.gateway

import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayConfig(
    value = [
        GatewayConfigGroup.SERVLET_SECURITY,
        GatewayConfigGroup.REFRESH_TOKEN_STORE,
    ],
)
class GatewayModuleConfig
