package com.beat.observability.logging

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class Log4j2PatternContractTest {

    @Test
    fun `application log pattern exposes request context as key value MDC fields`() {
        val log4j2 = Files.readString(Path.of("src/main/resources/log4j2-spring.xml"))

        assertTrue(log4j2.contains("[traceId=%equals{%X{traceId}}{}{NO_TRACE}]"))
        assertTrue(log4j2.contains("[userId=%equals{%X{userId}}{}{GUEST}]"))
        assertTrue(log4j2.contains("[clientIp=%equals{%X{clientIp}}{}{UNKNOWN}]"))
        assertTrue(log4j2.contains("[request=%equals{%X{requestInfo}}{}{NO_REQUEST}]"))
        assertTrue(log4j2.contains("[route=%equals{%X{routePattern}}{}{NO_ROUTE}]"))
    }
}
