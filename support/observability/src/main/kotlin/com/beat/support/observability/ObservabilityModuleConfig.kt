package com.beat.support.observability

import com.beat.support.observability.logging.LoggingConfig
import com.beat.support.observability.metrics.MetricsConfig
import com.beat.support.observability.sentry.SentryConfig
import com.beat.support.observability.tracing.TracingConfig
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
