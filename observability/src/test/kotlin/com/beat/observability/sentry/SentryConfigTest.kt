package com.beat.observability.sentry

import io.sentry.SentryOptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SentryConfigTest : FunSpec({

    test("disables Sentry SDK when DSN is blank") {
        val config = SentryConfig("beat-apis")
        val options = SentryOptions().apply {
            dsn = ""
            isEnabled = true
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        options.isEnabled shouldBe false
    }

    test("keeps Sentry enabled and registers processor when DSN exists") {
        val config = SentryConfig("beat-apis")
        val options = SentryOptions().apply {
            dsn = "https://public@example.ingest.sentry.io/1"
            isEnabled = true
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        options.isEnabled shouldBe true
        options.eventProcessors.any { it is BeatSentryEventProcessor } shouldBe true
    }

    test("uses configured release when SDK option has no release") {
        val config = SentryConfig("beat-apis", configuredRelease = "beat-server@abc123")
        val options = SentryOptions().apply {
            dsn = "https://public@example.ingest.sentry.io/1"
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        options.isEnabled shouldBe true
        options.release shouldBe "beat-server@abc123"
    }
})
