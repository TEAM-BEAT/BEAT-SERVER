package com.beat.apps.api.config

import com.beat.support.security.EnableGatewayConfig
import com.beat.support.security.EnableGatewayServletSecurity
import com.beat.support.security.GatewayConfigGroup
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayServletSecurity
@EnableGatewayConfig(
    value = [
        GatewayConfigGroup.GUEST_ACCESS,
    ],
)
class GatewayConfig
