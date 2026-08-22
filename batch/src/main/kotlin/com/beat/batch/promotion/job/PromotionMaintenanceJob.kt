package com.beat.batch.promotion.job

import com.beat.application.system.promotion.command.PromotionMaintenanceService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PromotionMaintenanceJob(
    private val promotionMaintenanceService: PromotionMaintenanceService,
    @param:Value("\${beat.scheduler.owner:false}") private val schedulerOwner: Boolean,
) {
    @Scheduled(cron = "1 0 0 * * ?", scheduler = "maintenanceTaskScheduler")
    fun checkAndDeleteInvalidPromotions() {
        if (!schedulerOwner) return
        promotionMaintenanceService.checkAndDeleteInvalidPromotions()
    }
}
