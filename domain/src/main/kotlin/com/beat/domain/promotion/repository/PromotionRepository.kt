package com.beat.domain.promotion.repository

import com.beat.domain.promotion.domain.CarouselNumber
import com.beat.domain.promotion.domain.Promotion
import java.util.*

@JvmSuppressWildcards
interface PromotionRepository {
    fun findAll(): List<Promotion>

    fun findById(promotionId: Long?): Optional<Promotion>

    fun save(promotion: Promotion): Promotion

    fun saveAll(promotions: List<Promotion>): List<Promotion>

    fun deleteByPromotionIds(promotionIds: List<Long>)

    fun deleteByPerformanceId(performanceId: Long?)

    fun findByCarouselNumber(carouselNumber: CarouselNumber): Optional<Promotion>
}
