package com.beat.admin.config

import com.beat.gateway.EnableGatewayServletSecurity
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayServletSecurity
class GatewayConfig
