package com.beat.observability.sentry

import io.sentry.metrics.IMetricsApi
import io.sentry.metrics.SentryMetricsParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BeatSentryMetricsTest {

    private val fakeMetrics = FakeMetricsApi()
    private val beatMetrics = BeatSentryMetrics(fakeMetrics)

    @Test
    fun `emits metrics through wrapper with controlled names and tags`() {
        beatMetrics.count("booking.created.count", tags = mapOf("module" to "apis"))
        beatMetrics.gauge("booking.queue_size", 42.0, tags = mapOf("securityLevel" to "internal"))
        beatMetrics.distribution("booking.amount.distribution", 15000.0)

        assertEquals(
            listOf(
                "count:booking.created.count:1.0:none",
                "gauge:booking.queue_size:42.0:none",
                "distribution:booking.amount.distribution:15000.0:none",
            ),
            fakeMetrics.calls,
        )
    }

    @Test
    fun `rejects invalid metric names and high cardinality tags`() {
        assertThrows(IllegalArgumentException::class.java) {
            beatMetrics.count("Booking Created")
        }
        assertThrows(IllegalArgumentException::class.java) {
            beatMetrics.count("booking.created.count", tags = mapOf("userId" to "42"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            beatMetrics.count("booking.created.count", tags = mapOf("rawUri" to "/api/bookings/42"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            beatMetrics.count("booking.created.count", tags = mapOf("requestUri" to "/api/bookings/42"))
        }
    }

    private class FakeMetricsApi : IMetricsApi {
        val calls = mutableListOf<String>()

        override fun count(key: String) = count(key, 1.0)
        override fun count(key: String, value: Double?) {
            calls += "count:$key:$value"
        }
        override fun count(key: String, unit: String?) = count(key, 1.0, unit)
        override fun count(key: String, value: Double?, unit: String?) {
            calls += "count:$key:$value:$unit"
        }
        override fun count(key: String, value: Double?, unit: String?, parameters: SentryMetricsParameters) = count(key, value, unit)

        override fun distribution(key: String, value: Double?) {
            calls += "distribution:$key:$value"
        }
        override fun distribution(key: String, value: Double?, unit: String?) {
            calls += "distribution:$key:$value:$unit"
        }
        override fun distribution(key: String, value: Double?, unit: String?, parameters: SentryMetricsParameters) = distribution(key, value, unit)

        override fun gauge(key: String, value: Double?) {
            calls += "gauge:$key:$value"
        }
        override fun gauge(key: String, value: Double?, unit: String?) {
            calls += "gauge:$key:$value:$unit"
        }
        override fun gauge(key: String, value: Double?, unit: String?, parameters: SentryMetricsParameters) = gauge(key, value, unit)
    }
}
