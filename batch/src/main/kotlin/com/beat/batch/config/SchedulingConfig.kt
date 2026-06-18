package com.beat.batch.config

import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Boot 오토컨피그가 생성하는 단일 `taskScheduler`(ThreadPoolTaskScheduler)에
 * [ScheduledTaskErrorHandler]를 주입한다.
 *
 * `ThreadPoolTaskSchedulerCustomizer`를 사용해 Boot의 기본 스케줄러 설정(스레드 풀 크기 등)은
 * 그대로 유지하고 ErrorHandler만 교체한다. 이 스케줄러는 `@Scheduled` 메서드와
 * `JobSchedulerService`가 주입받는 [org.springframework.scheduling.TaskScheduler] 동적 작업이
 * 공유하므로, 두 경로의 예외가 모두 동일한 핸들러로 수집된다.
 */
@Configuration(proxyBeanMethods = false)
class SchedulingConfig {

    @Bean
    fun scheduledTaskErrorHandler(): ScheduledTaskErrorHandler = ScheduledTaskErrorHandler()

    @Bean
    fun scheduledTaskErrorHandlerCustomizer(
        scheduledTaskErrorHandler: ScheduledTaskErrorHandler,
    ): ThreadPoolTaskSchedulerCustomizer =
        ThreadPoolTaskSchedulerCustomizer { taskScheduler ->
            taskScheduler.setErrorHandler(scheduledTaskErrorHandler)
        }
}
