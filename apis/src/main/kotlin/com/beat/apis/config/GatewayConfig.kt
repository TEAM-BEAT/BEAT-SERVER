package com.beat.apis.config

import com.beat.gateway.EnableGatewayConfig
import com.beat.gateway.GatewayConfigGroup
import com.beat.gateway.security.servlet.EnableGatewayServletSecurity
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayServletSecurity
@EnableGatewayConfig(
    value = [
        GatewayConfigGroup.REFRESH_TOKEN_STORE,
    ],
)
class GatewayConfig
