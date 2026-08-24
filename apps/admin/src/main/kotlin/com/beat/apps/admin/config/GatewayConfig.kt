package com.beat.apps.admin.config

import com.beat.support.security.EnableGatewayServletSecurity
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableGatewayServletSecurity
class GatewayConfig
