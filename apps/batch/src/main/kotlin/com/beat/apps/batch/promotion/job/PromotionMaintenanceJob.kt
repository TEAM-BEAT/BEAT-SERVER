package com.beat.apps.batch.promotion.job

import com.beat.application.system.promotion.command.PromotionMaintenanceService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PromotionMaintenanceJob(
    private val promotionMaintenanceService: PromotionMaintenanceService

) {
    @Scheduled(cron = "1 0 0 * * ?", scheduler = "maintenanceTaskScheduler")
    fun checkAndDeleteInvalidPromotions() {
        promotionMaintenanceService.checkAndDeleteInvalidPromotions()
    }
}
