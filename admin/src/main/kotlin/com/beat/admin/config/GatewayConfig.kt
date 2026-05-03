package com.beat.admin.config

import com.beat.gateway.EnableGatewayConfig
import com.beat.gateway.GatewayConfigGroup
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayConfig(
    value = [
        GatewayConfigGroup.SERVLET_SECURITY,
    ],
)
class GatewayConfig
