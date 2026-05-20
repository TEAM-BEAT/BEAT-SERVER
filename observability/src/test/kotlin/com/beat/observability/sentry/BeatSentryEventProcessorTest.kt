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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class BeatSentryEventProcessorTest {

    private val processor = BeatSentryEventProcessor("beat-apis")

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `enriches sentry events with MDC context and scrubs credentials`() {
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
        val processedUser = checkNotNull(processed.user)
        val processedRequest = checkNotNull(processed.request)
        val processedHeaders = checkNotNull(processedRequest.headers)
        val processedData = processedRequest.data as Map<*, *>

        assertEquals("beat-server", processed.getTag("service"))
        assertEquals("beat-apis", processed.getTag("module"))
        assertEquals("trace-123", processed.getTag(BaseMdcLoggingFilter.TRACE_ID_KEY))
        assertEquals("span-abc", processed.getTag(BaseMdcLoggingFilter.SPAN_ID_KEY))
        assertEquals("GET /api/performances/detail/{performanceId}", processed.getTag(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY))
        assertEquals("42", processedUser.id)
        assertEquals("10.0.0.1", processedUser.ipAddress)
        assertEquals(BeatSentryEventProcessor.REDACTED, processedHeaders["Authorization"])
        assertEquals(BeatSentryEventProcessor.REDACTED, processedHeaders["Cookie"])
        assertNull(processedRequest.cookies)
        assertEquals(BeatSentryEventProcessor.REDACTED, processedData["refreshToken"])
        assertEquals(BeatSentryEventProcessor.REDACTED, processed.getExtra("password"))
        assertEquals("bearer ${BeatSentryEventProcessor.REDACTED}", processed.getExtra("safe"))
    }

    @Test
    fun `enriches sentry logs with MDC attributes and scrubs sensitive log attributes`() {
        MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-456")
        MDC.put(BaseMdcLoggingFilter.ROUTE_PATTERN_KEY, "GET /api/main")

        val log = SentryLogEvent(SentryId(), 0.0, "Authorization=Bearer abc token=secret", SentryLogLevel.INFO)
        log.setAttribute("Authorization", SentryLogEventAttributeValue(SentryAttributeType.STRING, "Bearer abc"))
        log.setAttribute("business", SentryLogEventAttributeValue(SentryAttributeType.STRING, "booking"))

        val processed = processor.process(log)
        val processedAttributes = checkNotNull(processed.attributes)

        assertEquals("trace-456", processedAttributes[BaseMdcLoggingFilter.TRACE_ID_KEY]?.value)
        assertEquals("GET /api/main", processedAttributes[BaseMdcLoggingFilter.ROUTE_PATTERN_KEY]?.value)
        assertEquals(BeatSentryEventProcessor.REDACTED, processedAttributes["Authorization"]?.value)
        assertEquals("booking", processedAttributes["business"]?.value)
        assertTrue(processed.body.contains(BeatSentryEventProcessor.REDACTED))
    }
}
