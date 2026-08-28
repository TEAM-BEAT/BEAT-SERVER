package com.beat.apps.batch.config

import com.beat.apps.batch.scheduling.ScheduledTaskErrorHandler
import java.time.Duration
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * 작업군별로 격리된 명명 스케줄러를 등록한다.
 *
 * 스케줄 메서드에 `scheduler` qualifier(Spring Framework 6.1+)를 지정해 각 작업이 어떤 풀에서 실행될지 명시한다. 무거운 작업이 나중에
 * 추가되면 별도 스케줄러 빈(예: batchTaskScheduler)만 추가하고, 해당 작업의 `scheduler` 값만 바꾸면 된다. 현재는 정리성 작업만 있어
 * [maintenanceTaskScheduler] 하나만 둔다.
 *
 * `ThreadPoolTaskSchedulerBuilder`는 Boot가 커스텀 스케줄러 생성용으로 제공하는 공식 빈이다. `afterPropertiesSet()`이 컨테이너
 * 생명주기에서 자동 호출되어 초기화하므로 `initialize()`를 직접 호출하지 않는다.
 *
 * 모든 스케줄 메서드가 `scheduler`를 명시하는지는 `SchedulingConfigTest`가 애플리케이션 컨텍스트 전체를 스캔해 강제한다. 강제되므로 Boot 자동생성
 * `taskScheduler`용 [org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer]는 더 이상 두지 않는다.
 */
@Configuration(proxyBeanMethods = false)
class SchedulingConfig {

    @Bean fun scheduledTaskErrorHandler(): ScheduledTaskErrorHandler = ScheduledTaskErrorHandler()

    @Bean(name = ["maintenanceTaskScheduler"])
    fun maintenanceTaskScheduler(
        builder: ThreadPoolTaskSchedulerBuilder,
        scheduledTaskErrorHandler: ScheduledTaskErrorHandler,
    ): ThreadPoolTaskScheduler =
        builder
            .poolSize(2)
            .threadNamePrefix("maintenance-scheduler-")
            .awaitTermination(true)
            .awaitTerminationPeriod(Duration.ofSeconds(30))
            .additionalCustomizers({ it.setErrorHandler(scheduledTaskErrorHandler) })
            .build()
}
