package com.beat.observability.sentry

import io.sentry.SentryOptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SentryConfigTest : FunSpec({

    test("DSN이 blank이면 Sentry SDK를 비활성화한다") {
        val config = SentryConfig("beat-apis")
        val options = SentryOptions().apply {
            dsn = ""
            isEnabled = true
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        options.isEnabled shouldBe false
    }

    test("DSN이 있으면 Sentry를 활성화 상태로 유지하고 processor를 등록한다") {
        val config = SentryConfig("beat-apis")
        val options = SentryOptions().apply {
            dsn = "https://public@example.ingest.sentry.io/1"
            isEnabled = true
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        options.isEnabled shouldBe true
        options.eventProcessors.any { it is BeatSentryEventProcessor } shouldBe true
    }

    test("SDK option에 release가 없으면 설정된 release를 사용한다") {
        val config = SentryConfig("beat-apis", configuredRelease = "beat-server@abc123")
        val options = SentryOptions().apply {
            dsn = "https://public@example.ingest.sentry.io/1"
        }

        config.beatSentryOptionsConfiguration(config.beatSentryEventProcessor()).configure(options)

        options.isEnabled shouldBe true
        options.release shouldBe "beat-server@abc123"
    }
})
