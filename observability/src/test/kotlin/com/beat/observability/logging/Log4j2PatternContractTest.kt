package com.beat.observability.logging

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class Log4j2PatternContractTest : FunSpec() {

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

    init {
        test("LOG_PATTERN exposes all required MDC context fields") {
            val pattern = string("//Properties/Property[@name='LOG_PATTERN']")
            listOf("traceId", "spanId", "userId", "clientIp", "requestInfo", "routePattern").forEach { key ->
                pattern.contains("%X{$key}") shouldBe true
            }
        }

        test("SentryAppender is defined with ERROR as minimum event level") {
            val level = string("//Appenders/Sentry/@minimumEventLevel")
            level shouldBe "ERROR"
        }

        test("access logger is defined in prod and dev profiles and never references SentryAppender") {
            listOf("prod", "dev").forEach { profile ->
                val accessLoggers = nodeList(
                    "//springProfile[contains(@name,'$profile')]" +
                        "//Logger[@name='com.beat.observability.logging.access']",
                )
                (accessLoggers.length > 0) shouldBe true
            }

            val sentryRefs = nodeList(
                "//Logger[@name='com.beat.observability.logging.access']//AppenderRef[@ref='SentryAppender']",
            )
            sentryRefs.length shouldBe 0
        }

        test("com-beat business logger references SentryAppender in dev and prod profiles") {
            listOf("dev", "prod").forEach { profile ->
                val refs = nodeList(
                    "//springProfile[contains(@name,'$profile')]" +
                        "//Logger[@name='com.beat']" +
                        "//AppenderRef[@ref='SentryAppender']",
                )
                (refs.length > 0) shouldBe true
            }
        }

        test("root logger never references SentryAppender") {
            val rootRefs = nodeList("//Root//AppenderRef[@ref='SentryAppender']")
            rootRefs.length shouldBe 0
        }

        test("JsonConsoleAppender uses beat event template") {
            val uri = string("//Appenders/Console[@name='JsonConsoleAppender']/JsonTemplateLayout/@eventTemplateUri")
            uri shouldBe "classpath:beat-log-event-template.json"
        }

        test("prod profile uses always-JSON JsonConsoleAppender for all loggers") {
            listOf("com.beat.observability.logging.access", "com.beat").forEach { loggerName ->
                val refs = nodeList(
                    "//springProfile[contains(@name,'prod')]" +
                        "//Logger[@name='$loggerName']" +
                        "//AppenderRef[@ref='JsonConsoleAppender']",
                )
                (refs.length > 0) shouldBe true
            }
            val rootRef = nodeList(
                "//springProfile[contains(@name,'prod')]//Root//AppenderRef[@ref='JsonConsoleAppender']",
            )
            (rootRef.length > 0) shouldBe true
        }

        test("dev profile uses env-switching ConsoleAppender for all loggers") {
            listOf("com.beat.observability.logging.access", "com.beat").forEach { loggerName ->
                val refs = nodeList(
                    "//springProfile[contains(@name,'dev')]" +
                        "//Logger[@name='$loggerName']" +
                        "//AppenderRef[@ref='ConsoleAppender']",
                )
                (refs.length > 0) shouldBe true
            }
            val rootRef = nodeList(
                "//springProfile[contains(@name,'dev')]//Root//AppenderRef[@ref='ConsoleAppender']",
            )
            (rootRef.length > 0) shouldBe true
        }

        test("ConsoleAppender defaults to PatternLayout and switches to JSON only when BEAT_LOG_FORMAT=json") {
            // 로컬 기본값(env 없음): DefaultArbiter 가 PatternLayout — 개발자 무설정.
            val pretty = nodeList(
                "//Appenders/Console[@name='ConsoleAppender']//DefaultArbiter//PatternLayout",
            )
            (pretty.length > 0) shouldBe true

            // 배포(env=json): JsonTemplateLayout.
            val json = nodeList(
                "//Appenders/Console[@name='ConsoleAppender']" +
                    "//EnvironmentArbiter[@propertyName='BEAT_LOG_FORMAT'][@propertyValue='json']" +
                    "//JsonTemplateLayout",
            )
            (json.length > 0) shouldBe true
        }

        test("beat event template exposes all required fields for LogQL queries") {
            val template = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(File("src/main/resources/beat-log-event-template.json"))
            listOf(
                "trace_id", "span_id", "user_id", "client_ip",
                "request", "route", "http_status", "elapsed_ms",
                "level", "message", "@timestamp",
            ).forEach { field ->
                template.has(field) shouldBe true
            }
        }
    }
}
