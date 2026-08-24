package com.beat.infrastructure.persistence

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [InfraPersistenceMarker::class])
class InfraPersistenceConfig
