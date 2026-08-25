package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.AdminPromotionResults.AdminPromotionResult
import com.fasterxml.jackson.annotation.JsonProperty

data class CarouselHandleAllResponse(
    @get:JsonProperty("modifiedPromotions") val modifiedPromotionResponses: List<PromotionResponse>
) {
    constructor(
        promotionResults: AdminPromotionResults
    ) : this(promotionResults.promotionResults.map { PromotionResponse(it) })

    data class PromotionResponse(
        val promotionId: Long?,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val carouselNumber: String,
    ) {
        constructor(
            promotionResult: AdminPromotionResult
        ) : this(
            promotionId = promotionResult.promotionId,
            newImageUrl = promotionResult.newImageUrl,
            isExternal = promotionResult.isExternal,
            redirectUrl = promotionResult.redirectUrl,
            carouselNumber = promotionResult.carouselNumber,
        )
    }
}
