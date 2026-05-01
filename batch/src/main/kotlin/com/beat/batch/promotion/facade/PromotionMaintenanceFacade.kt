package com.beat.batch.promotion.facade

import com.beat.batch.promotion.application.PromotionMaintenanceService
import org.springframework.stereotype.Component

@Component
class PromotionMaintenanceFacade(
    private val promotionMaintenanceService: PromotionMaintenanceService,
) {

    fun checkAndDeleteInvalidPromotions() {
        promotionMaintenanceService.checkAndDeleteInvalidPromotions()
    }
}
