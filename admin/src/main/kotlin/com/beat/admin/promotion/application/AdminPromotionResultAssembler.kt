package com.beat.admin.promotion.application

import com.beat.admin.promotion.application.result.AdminPromotionResults
import com.beat.admin.promotion.application.result.AdminPromotionResults.AdminPromotionResult
import com.beat.domain.promotion.model.Promotion

object AdminPromotionResultAssembler {

    private val BY_CAROUSEL_NUMBER = compareBy<Promotion> { it.carouselNumber.ordinal }

    fun assemble(domainPromotions: List<Promotion>): AdminPromotionResults {
        val promotionResults = domainPromotions.sortedWith(BY_CAROUSEL_NUMBER).map { it.toPromotionResult() }
        return AdminPromotionResults(promotionResults)
    }

    private fun Promotion.toPromotionResult(): AdminPromotionResult = AdminPromotionResult(
        promotionId = requireNotNull(getId()),
        carouselNumber = carouselNumber.name,
        newImageUrl = promotionPhoto,
        isExternal = isExternal,
        redirectUrl = redirectUrl,
        performanceId = getPerformanceId(),
    )
}
