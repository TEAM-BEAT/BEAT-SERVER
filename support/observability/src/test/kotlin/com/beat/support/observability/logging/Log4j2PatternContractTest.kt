package com.beat.support.observability.logging

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.NodeList

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
        test("LOG_PATTERN은 필요한 모든 MDC context field를 노출한다") {
            val pattern = string("//Properties/Property[@name='LOG_PATTERN']")
            listOf("traceId", "spanId", "userId", "clientIp", "requestInfo", "routePattern")
                .forEach { key ->
                    pattern.contains("%X{$key}") shouldBe true
                }
        }

        test("SentryAppender는 minimum event level로 ERROR를 사용한다") {
            val level = string("//Appenders/Sentry/@minimumEventLevel")
            level shouldBe "ERROR"
        }

        test("access logger는 prod와 dev profile에 정의되고 SentryAppender를 참조하지 않는다") {
            listOf("prod", "dev").forEach { profile ->
                val accessLoggers =
                    nodeList(
                        "//springProfile[contains(@name,'$profile')]" +
                            "//Logger[@name='com.beat.support.observability.logging.access']"
                    )
                (accessLoggers.length > 0) shouldBe true
            }

            val sentryRefs =
                nodeList(
                    "//Logger[@name='com.beat.support.observability.logging.access']//AppenderRef[@ref='SentryAppender']"
                )
            sentryRefs.length shouldBe 0
        }

        test("com-beat business logger는 dev와 prod profile에서 SentryAppender를 참조한다") {
            listOf("dev", "prod").forEach { profile ->
                val refs =
                    nodeList(
                        "//springProfile[contains(@name,'$profile')]" +
                            "//Logger[@name='com.beat']" +
                            "//AppenderRef[@ref='SentryAppender']"
                    )
                (refs.length > 0) shouldBe true
            }
        }

        test("root logger는 SentryAppender를 참조하지 않는다") {
            val rootRefs = nodeList("//Root//AppenderRef[@ref='SentryAppender']")
            rootRefs.length shouldBe 0
        }

        test("JsonConsoleAppender는 beat event template을 사용한다") {
            val uri =
                string(
                    "//Appenders/Console[@name='JsonConsoleAppender']/JsonTemplateLayout/@eventTemplateUri"
                )
            uri shouldBe "classpath:beat-log-event-template.json"
        }

        test("prod profile은 모든 logger에 항상 JSON인 JsonConsoleAppender를 사용한다") {
            listOf("com.beat.support.observability.logging.access", "com.beat").forEach { loggerName
                ->
                val refs =
                    nodeList(
                        "//springProfile[contains(@name,'prod')]" +
                            "//Logger[@name='$loggerName']" +
                            "//AppenderRef[@ref='JsonConsoleAppender']"
                    )
                (refs.length > 0) shouldBe true
            }
            val rootRef =
                nodeList(
                    "//springProfile[contains(@name,'prod')]//Root//AppenderRef[@ref='JsonConsoleAppender']"
                )
            (rootRef.length > 0) shouldBe true
        }

        test("dev profile은 모든 logger에 env로 전환되는 ConsoleAppender를 사용한다") {
            listOf("com.beat.support.observability.logging.access", "com.beat").forEach { loggerName
                ->
                val refs =
                    nodeList(
                        "//springProfile[contains(@name,'dev')]" +
                            "//Logger[@name='$loggerName']" +
                            "//AppenderRef[@ref='ConsoleAppender']"
                    )
                (refs.length > 0) shouldBe true
            }
            val rootRef =
                nodeList(
                    "//springProfile[contains(@name,'dev')]//Root//AppenderRef[@ref='ConsoleAppender']"
                )
            (rootRef.length > 0) shouldBe true
        }

        test("ConsoleAppender는 기본값으로 PatternLayout을 사용하고 BEAT_LOG_FORMAT=json일 때만 JSON으로 전환한다") {
            // 로컬 기본값(env 없음): DefaultArbiter 가 PatternLayout — 개발자 무설정.
            val pretty =
                nodeList(
                    "//Appenders/Console[@name='ConsoleAppender']//DefaultArbiter//PatternLayout"
                )
            (pretty.length > 0) shouldBe true

            // 배포(env=json): JsonTemplateLayout.
            val json =
                nodeList(
                    "//Appenders/Console[@name='ConsoleAppender']" +
                        "//EnvironmentArbiter[@propertyName='BEAT_LOG_FORMAT'][@propertyValue='json']" +
                        "//JsonTemplateLayout"
                )
            (json.length > 0) shouldBe true
        }

        test("beat event template은 LogQL query에 필요한 모든 field를 노출한다") {
            val template =
                tools.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .readTree(File("src/main/resources/beat-log-event-template.json"))
            listOf(
                    "trace_id",
                    "span_id",
                    "user_id",
                    "client_ip",
                    "request",
                    "route",
                    "http_status",
                    "elapsed_ms",
                    "level",
                    "message",
                    "@timestamp",
                )
                .forEach { field ->
                    template.has(field) shouldBe true
                }
        }
    }
}
