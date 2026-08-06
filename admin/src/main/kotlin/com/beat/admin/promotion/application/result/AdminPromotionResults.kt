package com.beat.admin.promotion.application.result

data class AdminPromotionResults(
    val promotionResults: List<AdminPromotionResult>,
) {
    data class AdminPromotionResult(
        val promotionId: Long,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    )
}