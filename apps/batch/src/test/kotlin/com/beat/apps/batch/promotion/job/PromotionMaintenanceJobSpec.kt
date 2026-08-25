package com.beat.apps.batch.promotion.job

import com.beat.application.system.promotion.command.PromotionMaintenanceService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.annotation.Scheduled

class PromotionMaintenanceJobSpec :
    FunSpec({
        test("자정 1초 maintenance cron과 scheduler 계약을 유지한다") {
            val scheduled =
                PromotionMaintenanceJob::class
                    .java
                    .getDeclaredMethod("checkAndDeleteInvalidPromotions")
                    .getAnnotation(Scheduled::class.java)

            scheduled.cron shouldBe "1 0 0 * * ?"
            scheduled.scheduler shouldBe "maintenanceTaskScheduler"
        }

        test("checkAndDeleteInvalidPromotions는 System use case를 위임 호출한다") {
            val service = mockk<PromotionMaintenanceService>(relaxed = true)

            PromotionMaintenanceJob(service).checkAndDeleteInvalidPromotions()

            verify { service.checkAndDeleteInvalidPromotions() }
        }
    })
