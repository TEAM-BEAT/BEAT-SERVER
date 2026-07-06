package com.beat.batch.config

import com.beat.batch.support.AbstractBatchIntegrationTest
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.util.ReflectionTestUtils
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 실제 BatchApplication 컨텍스트에서 다음을 검증한다.
 *  1. 명명 스케줄러 빈 `maintenanceTaskScheduler`에 [ScheduledTaskErrorHandler]가 실제로 주입되었는지
 *     (= [SchedulingConfig]가 builder로 만든 스케줄러에 커스텀 에러 핸들러가 반영됨).
 *  2. `@Scheduled(scheduler = "maintenanceTaskScheduler")`와 동일한 스케줄러 빈을 사용하는 동적
 *     `taskScheduler.schedule(...)` 경로의 예외도 동일한 중앙 핸들러에서 기록되는지.
 *
 * 두 번째 검증은 `OutputCaptureExtension` 대신 프로그래매틱 Log4j2 appender로 확인한다.
 * (OutputCaptureExtension은 Log4j2 ConsoleAppender의 스트림 캐싱 때문에 전체 스위트 안에서 다른
 * 테스트와 함께 실행될 때 캡처가 누락될 수 있어, [ScheduledTaskErrorHandlerTest]와 동일하게
 * 로거에 직접 부착하는 방식으로 결정적으로 검증한다.)
 */
class SchedulingErrorHandlingIntegrationTest : AbstractBatchIntegrationTest() {

    @Autowired
    @Qualifier("maintenanceTaskScheduler")
    private lateinit var taskScheduler: TaskScheduler

    @Test
    fun maintenanceTaskSchedulerUsesCentralErrorHandler() {
        assertThat(ReflectionTestUtils.getField(taskScheduler, "errorHandler"))
            .isInstanceOf(ScheduledTaskErrorHandler::class.java)
    }

    @Test
    fun dynamicScheduledTaskFailureIsHandledByCentralErrorHandler() {
        val events = captureLogEvents(ScheduledTaskErrorHandler::class.java.name) { collected ->
            taskScheduler.schedule(
                { throw IllegalStateException("dynamic-schedule-boom") },
                Instant.now(),
            )

            await().atMost(Duration.ofSeconds(5)).until { collected().isNotEmpty() }
        }

        assertThat(events).hasSize(1)
        val event = events.first()
        assertThat(event.level).isEqualTo(Level.ERROR)
        assertThat(event.message.formattedMessage).isEqualTo("Batch task execution failed")
        assertThat(event.thrown).hasMessage("dynamic-schedule-boom")
    }

    /**
     * 프로그래매틱 Log4j2 appender로 특정 로거의 이벤트를 캡처한다.
     */
    private fun captureLogEvents(loggerName: String, block: (() -> List<LogEvent>) -> Unit): List<LogEvent> {
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
            block { appender.events.toList() }
            return appender.events.toList()
        } finally {
            loggerConfig.removeAppender("capture")
            appender.stop()
            context.updateLoggers()
        }
    }
}
