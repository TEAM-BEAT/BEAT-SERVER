package com.beat.observability.tracing

import io.micrometer.observation.ObservationFilter
import io.micrometer.tracing.Tracer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

@Configuration(proxyBeanMethods = false)
class TracingConfig {

    @Bean
    fun traceContextResolver(tracerProvider: ObjectProvider<Tracer>): TraceContextResolver {
        val tracer = tracerProvider.ifAvailable
        return tracer?.let(::MicrometerTraceContextResolver) ?: NoOpTraceContextResolver
    }

    @Bean
    @ConditionalOnClass(name = ["org.springframework.http.server.observation.ServerRequestObservationContext"])
    fun errorStatusObservationFilter(): ObservationFilter {
        return ObservationFilter { context ->
            if (context is ServerRequestObservationContext) {
                val response = context.response
                if (response != null && response.status >= 500 && context.error == null) {
                    context.setError(RuntimeException("HTTP ${response.status}"))
                }
            }
            context
        }
    }
}
