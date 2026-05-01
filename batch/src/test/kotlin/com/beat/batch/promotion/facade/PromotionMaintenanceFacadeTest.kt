package com.beat.batch.promotion.facade

import com.beat.batch.promotion.application.PromotionMaintenanceService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PromotionMaintenanceFacadeTest {

    @Test
    fun `maintenance facade delegates to application service`() {
        val promotionMaintenanceService = mock(PromotionMaintenanceService::class.java)
        val promotionMaintenanceFacade = PromotionMaintenanceFacade(promotionMaintenanceService)

        promotionMaintenanceFacade.checkAndDeleteInvalidPromotions()

        verify(promotionMaintenanceService).checkAndDeleteInvalidPromotions()
    }
}
