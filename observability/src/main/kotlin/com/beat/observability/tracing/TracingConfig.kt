package com.beat.observability.tracing

import io.micrometer.tracing.Tracer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class TracingConfig {

    @Bean
    fun traceContextResolver(tracerProvider: ObjectProvider<Tracer>): TraceContextResolver {
        val tracer = tracerProvider.ifAvailable
        return tracer?.let(::MicrometerTraceContextResolver) ?: NoOpTraceContextResolver
    }
}
