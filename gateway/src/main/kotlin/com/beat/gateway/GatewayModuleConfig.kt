package com.beat.gateway

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = ["com.beat.gateway"])
class GatewayModuleConfig
