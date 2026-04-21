package com.beat.observability

import org.springframework.context.annotation.Configuration

// Marker config for the observability module.
//
// This import keeps the existing activation semantics: executable modules opt into
// the observability module boundary, but this config does not component-scan the
// AOP package yet. When runtime logging aspects are intentionally activated,
// import that surface here together with a boot/context test.
@Configuration(proxyBeanMethods = false)
class ObservabilityModuleConfig
