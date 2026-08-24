package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.AdminPromotionResults.AdminPromotionResult
import com.fasterxml.jackson.annotation.JsonProperty

data class CarouselFindAllResponse(
    @get:JsonProperty("carousels")
    val carouselResponses: List<CarouselFindResponse>,
) {
    constructor(promotionResults: AdminPromotionResults) : this(
        promotionResults.promotionResults.map { CarouselFindResponse(it) },
    )

    data class CarouselFindResponse(
        val promotionId: Long?,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) {
        constructor(promotionResult: AdminPromotionResult) : this(
            promotionId = promotionResult.promotionId,
            carouselNumber = promotionResult.carouselNumber,
            newImageUrl = promotionResult.newImageUrl,
            isExternal = promotionResult.isExternal,
            redirectUrl = promotionResult.redirectUrl,
            performanceId = promotionResult.performanceId,
        )
    }
}
