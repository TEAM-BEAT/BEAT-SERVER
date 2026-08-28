package com.beat.support.observability.metrics

import com.beat.support.observability.metrics.config.ActuatorProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ActuatorProperties::class)
class MetricsConfig
