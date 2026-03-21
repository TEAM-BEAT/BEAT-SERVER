package com.beat.gateway

import com.beat.gateway.bootstrap.GatewayAuthBootstrapConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

// Gateway module entry point.
// Auth/security bootstrap ownership을 gateway 경계로 모은다.
@Configuration(proxyBeanMethods = false)
@Import(GatewayAuthBootstrapConfig::class)
class GatewayModuleConfig
