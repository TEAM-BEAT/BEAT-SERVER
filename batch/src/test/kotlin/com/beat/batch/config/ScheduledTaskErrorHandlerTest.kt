package com.beat.batch.config

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.CopyOnWriteArrayList

class ScheduledTaskErrorHandlerTest {

    private val handler = ScheduledTaskErrorHandler()

    @Test
    fun logsErrorWithThrowableAndDoesNotRethrow() {
        val failure = IllegalStateException("scheduled-boom")

        val events = captureLogEvents(ScheduledTaskErrorHandler::class.java.name) {
            assertDoesNotThrow { handler.handleError(failure) }
        }

        assertThat(events).hasSize(1)
        val event = events.first()
        assertThat(event.level).isEqualTo(Level.ERROR)
        assertThat(event.message.formattedMessage).isEqualTo("Batch task execution failed")
        assertThat(event.thrown).isSameAs(failure)
    }

    /**
     * 프로그래매틱 Log4j2 appender로 특정 로거의 이벤트를 캡처한다.
     * (OutputCaptureExtension은 Log4j2 ConsoleAppender의 스트림 캐싱 때문에 테스트 클래스 순서에
     * 따라 캡처가 누락될 수 있어, 백엔드에 직접 부착하는 방식으로 결정적으로 검증한다.)
     */
    private fun captureLogEvents(loggerName: String, block: () -> Unit): List<LogEvent> {
        val context = LogManager.getContext(false) as LoggerContext
        val configuration = context.configuration
        val appender = object : AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
            val events = CopyOnWriteArrayList<LogEvent>()
            override fun append(event: LogEvent) {
                events.add(event.toImmutable())
            }
        }
        appender.start()
        configuration.addAppender(appender)
        val loggerConfig = configuration.getLoggerConfig(loggerName)
        loggerConfig.addAppender(appender, Level.ERROR, null)
        context.updateLoggers()
        try {
            block()
            return appender.events.toList()
        } finally {
            loggerConfig.removeAppender("capture")
            appender.stop()
            context.updateLoggers()
        }
    }
}
