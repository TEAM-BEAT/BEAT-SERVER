package com.beat.apis.config

import com.beat.gateway.EnableGatewayConfig
import com.beat.gateway.GatewayConfigGroup
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayConfig(
    value = [
        GatewayConfigGroup.SERVLET_SECURITY,
        GatewayConfigGroup.REFRESH_TOKEN_STORE,
    ],
)
class GatewayConfig
