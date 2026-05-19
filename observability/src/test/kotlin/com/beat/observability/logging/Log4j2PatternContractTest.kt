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

    // ── Phase 1: PatternLayout MDC contract ──────────────────────────────────

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
    fun `access logger is defined in prod and dev profiles and never connected to SentryAppender`() {
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
    fun `audit logger is defined as placeholder in prod and dev profiles`() {
        listOf("prod", "dev").forEach { profile ->
            val auditLoggers = nodeList(
                "//springProfile[contains(@name,'$profile')]//Logger[@name='audit.beat']",
            )
            assertTrue(auditLoggers.length > 0, "audit.beat logger must be defined in $profile profile")
        }
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

    // ── Phase 2: JSON layout contract ─────────────────────────────────────────

    @Test
    fun `JsonConsoleAppender is defined and uses beat event template`() {
        val uri = string("//Appenders/Console[@name='JsonConsoleAppender']/JsonTemplateLayout/@eventTemplateUri")
        assertEquals(
            "classpath:beat-log-event-template.json",
            uri,
            "JsonConsoleAppender must reference beat-log-event-template.json",
        )
    }

    @Test
    fun `prod profile uses JsonConsoleAppender for all loggers`() {
        listOf("com.beat.observability.logging.access", "audit.beat", "com.beat").forEach { loggerName ->
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
    fun `non-prod profiles do not use JsonConsoleAppender`() {
        listOf("dev", "test").forEach { profile ->
            val refs = nodeList(
                "//springProfile[contains(@name,'$profile')]//AppenderRef[@ref='JsonConsoleAppender']",
            )
            assertEquals(
                0,
                refs.length,
                "$profile profile must not use JsonConsoleAppender — PatternLayout is for human readability",
            )
        }
    }

    @Test
    fun `beat event template exposes all required fields for LogQL queries`() {
        // Parse to a real JSON tree so the contract validates top-level structure, not raw text.
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
