package com.beat.admin.config

import com.beat.gateway.security.servlet.EnableGatewayServletSecurity
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayServletSecurity
class GatewayConfig
