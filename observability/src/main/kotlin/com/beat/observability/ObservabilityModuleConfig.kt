package com.beat.observability

import org.springframework.context.annotation.Configuration

// Marker config for the observability module.
// Next step: collect logging, metrics, tracing, and actuator-related imports here
// so runtimes can opt into a single observability boundary explicitly.
@Configuration(proxyBeanMethods = false)
class ObservabilityModuleConfig
