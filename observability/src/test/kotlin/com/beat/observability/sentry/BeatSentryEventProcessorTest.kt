package com.beat.observability.sentry

import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import io.sentry.Hint
import io.sentry.SentryAttributeType
import io.sentry.SentryEvent
import io.sentry.SentryLogEvent
import io.sentry.SentryLogEventAttributeValue
import io.sentry.SentryLogLevel
import io.sentry.protocol.Request
import io.sentry.protocol.SentryId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.slf4j.MDC

class BeatSentryEventProcessorTest : FunSpec() {

    private val processor = BeatSentryEventProcessor("beat-apis")

    init {
        afterTest { MDC.clear() }

        test("sentry event에 MDC context를 enrich하고 credential은 scrub한다") {
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123")
            MDC.put(BaseMdcLoggingFilter.SPAN_ID_KEY, "span-abc")
            MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, "42")
            MDC.put(BaseMdcLoggingFilter.CLIENT_IP_KEY, "10.0.0.1")
            MDC.put(BaseMdcLoggingFilter.REQUEST_INFO_KEY, "GET /api/performances/detail/19")
            MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/performances/detail/{performanceId}")

            val event = SentryEvent().apply {
                request = Request().apply {
                    headers = mapOf(
                        "Authorization" to "Bearer secret.jwt.value",
                        "Cookie" to "SESSION=abc",
                        "X-Request-ID" to "trace-123",
                    )
                    cookies = "SESSION=abc"
                    data = mapOf("refreshToken" to "token-value", "safe" to "value")
                }
                setExtra("password", "plain-secret")
                setExtra("safe", "bearer token=abc")
            }

            val processed = processor.process(event, Hint())
            val processedUser = processed.user.shouldNotBeNull()
            val processedRequest = processed.request.shouldNotBeNull()
            val processedHeaders = processedRequest.headers.shouldNotBeNull()
            val processedData = processedRequest.data.shouldNotBeNull() as Map<*, *>

            processed.getTag("service") shouldBe "beat-server"
            processed.getTag("module") shouldBe "beat-apis"
            processed.getTag(BaseMdcLoggingFilter.TRACE_ID_KEY) shouldBe "trace-123"
            processed.getTag(BaseMdcLoggingFilter.SPAN_ID_KEY) shouldBe "span-abc"
            processed.getTag(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY) shouldBe "GET /api/performances/detail/{performanceId}"
            processedUser.id shouldBe "42"
            processedUser.ipAddress shouldBe "10.0.0.1"
            processedHeaders["Authorization"] shouldBe BeatSentryEventProcessor.REDACTED
            processedHeaders["Cookie"] shouldBe BeatSentryEventProcessor.REDACTED
            processedRequest.cookies.shouldBeNull()
            processedData["refreshToken"] shouldBe BeatSentryEventProcessor.REDACTED
            processed.getExtra("password") shouldBe BeatSentryEventProcessor.REDACTED
            processed.getExtra("safe") shouldBe "bearer ${BeatSentryEventProcessor.REDACTED}"
        }

        test("sentry log에 MDC attribute를 enrich하고 민감한 log attribute는 scrub한다") {
            MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-456")
            MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/main")

            val log = SentryLogEvent(SentryId(), 0.0, "Authorization=Bearer abc token=secret", SentryLogLevel.INFO)
            log.setAttribute("Authorization", SentryLogEventAttributeValue(SentryAttributeType.STRING, "Bearer abc"))
            log.setAttribute("business", SentryLogEventAttributeValue(SentryAttributeType.STRING, "booking"))

            val processed = processor.process(log)
            val processedAttributes = processed.attributes.shouldNotBeNull()

            processedAttributes[BaseMdcLoggingFilter.TRACE_ID_KEY]?.value shouldBe "trace-456"
            processedAttributes[BaseMdcLoggingFilter.ROUTE_PATTERN_KEY]?.value shouldBe "GET /api/main"
            processedAttributes["Authorization"]?.value shouldBe BeatSentryEventProcessor.REDACTED
            processedAttributes["business"]?.value shouldBe "booking"
            processed.body.contains(BeatSentryEventProcessor.REDACTED) shouldBe true
        }
    }
}
