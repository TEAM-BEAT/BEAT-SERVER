package com.beat.application.admin.promotion

data class AdminPromotionResults(val promotionResults: List<AdminPromotionResult>) {
    data class AdminPromotionResult(
        val promotionId: Long,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    )
}
