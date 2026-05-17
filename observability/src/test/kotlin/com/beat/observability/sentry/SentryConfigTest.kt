package com.beat.observability.sentry

import io.sentry.SentryOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SentryConfigTest {

    @Test
    fun `disables Sentry SDK when DSN is blank`() {
        val config = SentryConfig("beat-apis")
        val options = SentryOptions().apply {
            dsn = ""
            isEnabled = true
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        assertFalse(options.isEnabled)
    }

    @Test
    fun `keeps Sentry enabled and registers processor when DSN exists`() {
        val config = SentryConfig("beat-apis")
        val options = SentryOptions().apply {
            dsn = "https://public@example.ingest.sentry.io/1"
            isEnabled = true
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        assertTrue(options.isEnabled)
        assertTrue(options.eventProcessors.any { it is BeatSentryEventProcessor })
    }

    @Test
    fun `uses configured release when SDK option has no release`() {
        val config = SentryConfig("beat-apis", configuredRelease = "beat-server@abc123")
        val options = SentryOptions().apply {
            dsn = "https://public@example.ingest.sentry.io/1"
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        assertTrue(options.isEnabled)
        assertEquals("beat-server@abc123", options.release)
    }
}
