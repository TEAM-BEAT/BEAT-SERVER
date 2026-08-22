package com.beat.batch.promotion.job

import com.beat.application.system.promotion.command.PromotionMaintenanceService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.scheduling.annotation.Scheduled

class PromotionMaintenanceJobSpec : FunSpec({
    test("자정 1초 maintenance cron과 scheduler 계약을 유지한다") {
        val scheduled = PromotionMaintenanceJob::class.java
            .getDeclaredMethod("checkAndDeleteInvalidPromotions")
            .getAnnotation(Scheduled::class.java)

        scheduled.cron shouldBe "1 0 0 * * ?"
        scheduled.scheduler shouldBe "maintenanceTaskScheduler"
    }

    test("현재 runtime이 scheduler owner이면 System use case를 호출한다") {
        val service = mock(PromotionMaintenanceService::class.java)

        PromotionMaintenanceJob(service, true).checkAndDeleteInvalidPromotions()

        verify(service).checkAndDeleteInvalidPromotions()
    }

    test("현재 runtime이 scheduler owner가 아니면 실행하지 않는다") {
        val service = mock(PromotionMaintenanceService::class.java)

        PromotionMaintenanceJob(service, false).checkAndDeleteInvalidPromotions()

        verifyNoInteractions(service)
    }
})
