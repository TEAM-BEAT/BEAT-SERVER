package com.beat.batch.config

import org.slf4j.LoggerFactory
import org.springframework.util.ErrorHandler

/**
 * batch TaskScheduler 전역 에러 핸들러.
 *
 * 기본 Spring 핸들러(TaskUtils$LoggingErrorHandler)는 `org.springframework.*` 로거로 예외를 남겨
 * `com.beat` 로거에만 연결된 SentryAppender 경로를 타지 못한다. 이 핸들러는 `com.beat.batch.*`
 * 로거로 ERROR 로그를 남겨 Sentry(SentryAppender) + stdout(→Loki) 양쪽 수집 경로를 복구한다.
 *
 * `@Scheduled` 메서드와 [org.springframework.scheduling.TaskScheduler] 동적 `schedule(...)` 작업의
 * 예외가 모두 동일한 스케줄러 빈을 통해 이 핸들러로 전달된다.
 *
 * 예외를 재전파하지 않으므로(no-rethrow) 반복 작업(fixedDelay/fixedRate/cron)은 다음 주기에 계속
 * 실행되는 Spring 기본 동작(suppress)을 그대로 유지한다.
 */
class ScheduledTaskErrorHandler : ErrorHandler {

    private val log = LoggerFactory.getLogger(ScheduledTaskErrorHandler::class.java)

    override fun handleError(t: Throwable) {
        log.error("Batch task execution failed", t)
    }
}
