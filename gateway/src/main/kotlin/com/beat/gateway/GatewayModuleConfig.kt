package com.beat.gateway

import org.springframework.context.annotation.Configuration

// Marker config for the gateway module.
// Next step: gather security/auth-related imports here so runtimes can depend on
// an explicit gateway boundary instead of broad package scanning.
@Configuration(proxyBeanMethods = false)
class GatewayModuleConfig
