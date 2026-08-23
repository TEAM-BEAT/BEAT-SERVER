package com.beat.observability.sentry

import io.sentry.Sentry
import io.sentry.metrics.IMetricsApi
import io.sentry.metrics.SentryMetricsParameters

class BeatSentryMetrics(
    private val metrics: IMetricsApi = Sentry.metrics(),
) {

    fun count(
        name: String,
        value: Double = 1.0,
        tags: Map<String, String> = emptyMap(),
    ) {
        metrics.count(requireMetricName(name), value, UNIT_NONE, parameters(tags))
    }

    fun gauge(
        name: String,
        value: Double,
        tags: Map<String, String> = emptyMap(),
    ) {
        metrics.gauge(requireMetricName(name), value, UNIT_NONE, parameters(tags))
    }

    fun distribution(
        name: String,
        value: Double,
        tags: Map<String, String> = emptyMap(),
    ) {
        metrics.distribution(requireMetricName(name), value, UNIT_NONE, parameters(tags))
    }

    private fun requireMetricName(name: String): String {
        require(metricNamePattern.matches(name)) {
            "Sentry metric name must match ${metricNamePattern.pattern}: $name"
        }
        return name
    }

    private fun parameters(tags: Map<String, String>): SentryMetricsParameters {
        requireNoForbiddenTags(tags)
        return SentryMetricsParameters.create(tags)
    }

    private fun requireNoForbiddenTags(tags: Map<String, String>) {
        tags.keys.firstOrNull(::isForbiddenTag)?.let { forbidden ->
            throw IllegalArgumentException("Sentry metric tag has forbidden high-cardinality/sensitive key: $forbidden")
        }
    }

    private fun isForbiddenTag(key: String): Boolean =
        SentrySensitiveDataPolicy.isForbiddenMetricTag(key)

    companion object {
        private const val UNIT_NONE = "none"
        private val metricNamePattern = Regex("^[a-z][a-z0-9_.-]{0,127}$")
    }
}
