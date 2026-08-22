package com.beat.batch.config

import com.beat.batch.support.BeatBatchAcceptanceTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
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
@BeatBatchAcceptanceTest
@Tags("acceptance")
class SchedulingConfigTest : FunSpec() {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var maintenanceTaskScheduler: ThreadPoolTaskScheduler

    @Autowired
    private lateinit var scheduledTaskErrorHandler: ScheduledTaskErrorHandler

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("maintenanceTaskScheduler는_지정한_설정으로_구성된다") {
            maintenanceTaskScheduler.poolSize shouldBe 2
            maintenanceTaskScheduler.threadNamePrefix shouldBe "maintenance-scheduler-"
            ReflectionTestUtils.getField(maintenanceTaskScheduler, "errorHandler") shouldBe scheduledTaskErrorHandler
        }

        test("모든_Scheduled_메서드는_scheduler를_명시한다") {
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

            violations shouldBe emptyList()
        }
    }
}
