package com.beat.observability

import com.beat.observability.logging.LoggingConfig
import com.beat.observability.metrics.MetricsConfig
import com.beat.observability.sentry.SentryConfig
import com.beat.observability.tracing.TracingConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(
    LoggingConfig::class,
    MetricsConfig::class,
    TracingConfig::class,
    SentryConfig::class,
)
class ObservabilityModuleConfig
