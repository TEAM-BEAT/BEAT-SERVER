package com.beat.apps.batch

import com.beat.application.system.booking.command.TicketCleanupService
import com.beat.application.system.promotion.command.PromotionMaintenanceService
import com.beat.apps.batch.support.BeatBatchAcceptanceTest
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

        test("test runtime은 System use case와 persistence를 조립한다") {
            applicationContext.containsBean("taskScheduler") shouldBe false
            applicationContext.containsBean("maintenanceTaskScheduler") shouldBe true
            applicationContext.getBeansOfType(TaskScheduler::class.java).size shouldBe 1
            applicationContext.getBeansOfType(TicketCleanupService::class.java).size shouldBe 1
            applicationContext.getBeansOfType(PromotionMaintenanceService::class.java).size shouldBe 1
            applicationContext.getBeansOfType(PromotionRepository::class.java).size shouldBe 1
            applicationContext.getBeansOfType(ScheduleRepository::class.java).size shouldBe 1
        }
    }
}
