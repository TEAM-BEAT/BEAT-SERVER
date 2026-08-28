package com.beat.support.observability.metrics.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "management.endpoints.web")
data class ActuatorProperties(val basePath: String)
