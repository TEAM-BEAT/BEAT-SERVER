package com.beat.batch

import com.beat.application.system.booking.command.TicketCleanupService
import com.beat.application.system.promotion.command.PromotionMaintenanceService
import com.beat.batch.booking.job.TicketCleanupJob
import com.beat.batch.promotion.job.PromotionMaintenanceJob
import com.beat.batch.support.BeatBatchAcceptanceTest
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.util.ReflectionTestUtils

@BeatBatchAcceptanceTest
@Tags("acceptance")
class BatchModuleContextBootSpec : FunSpec() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var environment: Environment

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("test runtime은 scheduler ownership 없이 System use case와 persistence를 조립한다") {
            environment.getProperty("beat.scheduler.owner") shouldBe "false"
            applicationContext.containsBean("taskScheduler") shouldBe false
            applicationContext.containsBean("maintenanceTaskScheduler") shouldBe true
            applicationContext.getBeansOfType(TaskScheduler::class.java).size shouldBe 1
            ReflectionTestUtils.getField(
                applicationContext.getBean(TicketCleanupJob::class.java),
                "schedulerOwner",
            ) shouldBe false
            ReflectionTestUtils.getField(
                applicationContext.getBean(PromotionMaintenanceJob::class.java),
                "schedulerOwner",
            ) shouldBe false
            applicationContext.getBeansOfType(TicketCleanupService::class.java).size shouldBe 1
            applicationContext.getBeansOfType(PromotionMaintenanceService::class.java).size shouldBe 1
            applicationContext.getBeansOfType(PromotionRepository::class.java).size shouldBe 1
            applicationContext.getBeansOfType(ScheduleRepository::class.java).size shouldBe 1
        }
    }
}
