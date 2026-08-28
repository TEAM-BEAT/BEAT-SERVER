package com.beat.application.system.promotion.command

import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import com.beat.domain.promotion.service.PromotionEligibilityDomainService
import com.beat.domain.schedule.repository.ScheduleRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PromotionMaintenanceService
internal constructor(
    private val promotionRepository: PromotionRepository,
    private val performanceRepository: PerformanceRepository,
    private val scheduleRepository: ScheduleRepository,
    private val promotionCarouselDomainService: PromotionCarouselDomainService,
    private val promotionEligibilityDomainService: PromotionEligibilityDomainService,
    private val clock: Clock,
) {
    @Transactional
    fun checkAndDeleteInvalidPromotions() {
        val discoveredPerformanceIds =
            promotionRepository.findAll().mapNotNull(Promotion::performanceId).distinct().sorted()
        val performanceDates = lockAuthoritativePerformanceDates(discoveredPerformanceIds)
        val authoritativePromotions = promotionRepository.lockAll()
        val promotionIdsToDelete =
            authoritativePromotions
                .filter { promotion -> isInvalidPromotion(promotion, performanceDates) }
                .mapNotNull(Promotion::id)

        if (promotionIdsToDelete.isEmpty()) return

        log.info { "Deleting promotions: $promotionIdsToDelete" }
        promotionRepository.deleteByPromotionIds(promotionIdsToDelete)
        val remainingPromotions = authoritativePromotions.filterNot {
            it.id in promotionIdsToDelete
        }
        promotionRepository.saveAll(
            promotionCarouselDomainService.arrangeCarouselNumbers(remainingPromotions)
        )
    }

    private fun lockAuthoritativePerformanceDates(
        performanceIds: List<Long>
    ): Map<Long, List<LocalDateTime>> =
        performanceIds
            .mapNotNull { performanceId ->
                val performance =
                    performanceRepository.lockById(performanceId) ?: return@mapNotNull null
                val lockedPerformanceId = checkNotNull(performance.id)
                val scheduleIds =
                    scheduleRepository
                        .findIdsByPerformanceId(lockedPerformanceId)
                        .distinct()
                        .sorted()
                val dates = scheduleIds.mapNotNull { scheduleId ->
                    scheduleRepository
                        .lockById(scheduleId)
                        ?.takeIf { it.belongsTo(lockedPerformanceId) }
                        ?.performanceDate
                }
                lockedPerformanceId to dates
            }
            .toMap()

    private fun isInvalidPromotion(
        promotion: Promotion,
        performanceDates: Map<Long, List<LocalDateTime>>,
    ): Boolean {
        val performanceId = promotion.performanceId ?: return false
        val dates = performanceDates[performanceId] ?: return false
        return !promotionEligibilityDomainService.isEligible(
            promotion,
            dates,
            LocalDate.now(clock),
        )
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }
}
