package com.beat.domain.promotion.service

import com.beat.domain.promotion.model.Promotion
import java.time.LocalDate
import java.time.LocalDateTime

class PromotionEligibilityDomainService {
    fun isEligible(
        promotion: Promotion,
        performanceDates: List<LocalDateTime>,
        today: LocalDate,
    ): Boolean {
        if (promotion.getPerformanceId() == null || performanceDates.isEmpty()) {
            return true
        }

        return performanceDates.any { !it.toLocalDate().isBefore(today) }
    }
}
