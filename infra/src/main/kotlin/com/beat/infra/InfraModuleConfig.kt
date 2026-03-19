package com.beat.infra

import org.springframework.context.annotation.Configuration

// Marker config for the infra module.
// Next step: keep growing this as the single entry point for infra bootstrapping
// such as JPA, Redis, QueryDSL, async, and other technical adapters.
@Configuration(proxyBeanMethods = false)
class InfraModuleConfig
