package com.beat.batch.config

import com.beat.batch.support.AbstractBatchIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.test.util.ReflectionTestUtils

/**
 * 명명 스케줄러 구성과 `@Scheduled(scheduler = "...")` 사용 규칙을 검증한다.
 *
 * 1. `maintenanceTaskScheduler` 빈이 지정한 풀 크기·스레드 이름 접두사·에러 핸들러로 구성되는지.
 * 2. 애플리케이션 컨텍스트에 등록된 모든 `@Scheduled` 메서드가 `scheduler`를 명시하는지 — 하드코딩된
 *    메서드 목록이 아니라 컨텍스트 전체를 스캔해 검증하므로, 새 스케줄 작업이 추가되면서 `scheduler`
 *    속성을 빠뜨려도 이 테스트가 잡아낸다. Boot 자동생성 `taskScheduler`용
 *    [org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer]를 더 이상 두지 않으므로,
 *    `scheduler`를 빠뜨린 메서드는 에러 핸들러가 적용되지 않는 이름 없는 스케줄러로 조용히
 *    fallback되어 예외가 관측되지 않을 수 있다.
 */
class SchedulingConfigTest : AbstractBatchIntegrationTest() {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var maintenanceTaskScheduler: ThreadPoolTaskScheduler

    @Autowired
    private lateinit var scheduledTaskErrorHandler: ScheduledTaskErrorHandler

    @Test
    fun maintenanceTaskScheduler는_지정한_설정으로_구성된다() {
        assertThat(maintenanceTaskScheduler.poolSize).isEqualTo(2)
        assertThat(maintenanceTaskScheduler.threadNamePrefix).isEqualTo("maintenance-scheduler-")
        assertThat(ReflectionTestUtils.getField(maintenanceTaskScheduler, "errorHandler"))
            .isSameAs(scheduledTaskErrorHandler)
    }

    @Test
    fun 모든_Scheduled_메서드는_scheduler를_명시한다() {
        val violations = applicationContext.beanDefinitionNames
            .mapNotNull { beanName ->
                runCatching { applicationContext.getType(beanName) }.getOrNull()
            }
            .flatMap { type ->
                type.methods
                    .filter { it.isAnnotationPresent(Scheduled::class.java) }
                    .filter { it.getAnnotation(Scheduled::class.java).scheduler.isBlank() }
                    .map { "${type.simpleName}.${it.name}" }
            }
            .distinct()

        assertThat(violations)
            .withFailMessage("scheduler 속성이 없는 @Scheduled 메서드: %s", violations)
            .isEmpty()
    }
}
