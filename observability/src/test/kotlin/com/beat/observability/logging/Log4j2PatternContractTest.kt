package com.beat.observability.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class Log4j2PatternContractTest {

    private val doc by lazy {
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/resources/log4j2-spring.xml"))
    }

    private val xpath = XPathFactory.newInstance().newXPath()

    private fun string(expr: String): String =
        xpath.evaluate(expr, doc, XPathConstants.STRING) as String

    private fun nodeList(expr: String): NodeList =
        xpath.evaluate(expr, doc, XPathConstants.NODESET) as NodeList

    @Test
    fun `LOG_PATTERN exposes all required MDC context fields`() {
        val pattern = string("//Properties/Property[@name='LOG_PATTERN']")
        listOf("traceId", "spanId", "userId", "clientIp", "requestInfo", "routePattern").forEach { key ->
            assertTrue(pattern.contains("%X{$key}"), "LOG_PATTERN must include MDC key: $key")
        }
    }

    @Test
    fun `SentryAppender is defined with ERROR as minimum event level`() {
        val level = string("//Appenders/Sentry/@minimumEventLevel")
        assertEquals("ERROR", level, "SentryAppender minimumEventLevel must be ERROR")
    }

    @Test
    fun `access logger is defined in prod and dev profiles and never references SentryAppender`() {
        listOf("prod", "dev").forEach { profile ->
            val accessLoggers = nodeList(
                "//springProfile[contains(@name,'$profile')]" +
                    "//Logger[@name='com.beat.observability.logging.access']",
            )
            assertTrue(accessLoggers.length > 0, "access logger must be defined in $profile profile")
        }

        val sentryRefs = nodeList(
            "//Logger[@name='com.beat.observability.logging.access']//AppenderRef[@ref='SentryAppender']",
        )
        assertEquals(0, sentryRefs.length, "access logger must not reference SentryAppender in any profile")
    }

    @Test
    fun `com-beat business logger references SentryAppender in dev and prod profiles`() {
        listOf("dev", "prod").forEach { profile ->
            val refs = nodeList(
                "//springProfile[contains(@name,'$profile')]" +
                    "//Logger[@name='com.beat']" +
                    "//AppenderRef[@ref='SentryAppender']",
            )
            assertTrue(refs.length > 0, "com.beat logger must reference SentryAppender in $profile profile")
        }
    }

    @Test
    fun `root logger never references SentryAppender`() {
        val rootRefs = nodeList("//Root//AppenderRef[@ref='SentryAppender']")
        assertEquals(
            0,
            rootRefs.length,
            "Root logger must not reference SentryAppender — third-party ERRORs must not flood Sentry",
        )
    }

    @Test
    fun `JsonConsoleAppender uses beat event template`() {
        val uri = string("//Appenders/Console[@name='JsonConsoleAppender']/JsonTemplateLayout/@eventTemplateUri")
        assertEquals("classpath:beat-log-event-template.json", uri)
    }

    @Test
    fun `prod profile uses always-JSON JsonConsoleAppender for all loggers`() {
        listOf("com.beat.observability.logging.access", "com.beat").forEach { loggerName ->
            val refs = nodeList(
                "//springProfile[contains(@name,'prod')]" +
                    "//Logger[@name='$loggerName']" +
                    "//AppenderRef[@ref='JsonConsoleAppender']",
            )
            assertTrue(refs.length > 0, "$loggerName must use JsonConsoleAppender in prod profile")
        }
        val rootRef = nodeList(
            "//springProfile[contains(@name,'prod')]//Root//AppenderRef[@ref='JsonConsoleAppender']",
        )
        assertTrue(rootRef.length > 0, "Root must use JsonConsoleAppender in prod profile")
    }

    @Test
    fun `dev profile uses env-switching ConsoleAppender for all loggers`() {
        listOf("com.beat.observability.logging.access", "com.beat").forEach { loggerName ->
            val refs = nodeList(
                "//springProfile[contains(@name,'dev')]" +
                    "//Logger[@name='$loggerName']" +
                    "//AppenderRef[@ref='ConsoleAppender']",
            )
            assertTrue(refs.length > 0, "$loggerName must use ConsoleAppender in dev profile")
        }
        val rootRef = nodeList(
            "//springProfile[contains(@name,'dev')]//Root//AppenderRef[@ref='ConsoleAppender']",
        )
        assertTrue(rootRef.length > 0, "Root must use ConsoleAppender in dev profile")
    }

    @Test
    fun `ConsoleAppender defaults to PatternLayout and switches to JSON only when BEAT_LOG_FORMAT=json`() {
        // 로컬 기본값(env 없음): DefaultArbiter 가 PatternLayout — 개발자 무설정.
        val pretty = nodeList(
            "//Appenders/Console[@name='ConsoleAppender']//DefaultArbiter//PatternLayout",
        )
        assertTrue(pretty.length > 0, "ConsoleAppender must default to PatternLayout (local plain text)")

        // 배포(env=json): JsonTemplateLayout.
        val json = nodeList(
            "//Appenders/Console[@name='ConsoleAppender']" +
                "//EnvironmentArbiter[@propertyName='BEAT_LOG_FORMAT'][@propertyValue='json']" +
                "//JsonTemplateLayout",
        )
        assertTrue(json.length > 0, "ConsoleAppender must switch to JsonTemplateLayout under BEAT_LOG_FORMAT=json")
    }

    @Test
    fun `beat event template exposes all required fields for LogQL queries`() {
        val template = tools.jackson.databind.json.JsonMapper.builder().build()
            .readTree(File("src/main/resources/beat-log-event-template.json"))
        listOf(
            "trace_id", "span_id", "user_id", "client_ip",
            "request", "route", "http_status", "elapsed_ms",
            "level", "message", "@timestamp",
        ).forEach { field ->
            assertTrue(
                template.has(field),
                "beat-log-event-template.json must define top-level field: $field",
            )
        }
    }
}
