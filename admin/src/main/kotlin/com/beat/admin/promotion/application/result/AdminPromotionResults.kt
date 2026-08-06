package com.beat.admin.promotion.application.result

@JvmRecord
data class AdminPromotionResults(
    val promotionResults: List<AdminPromotionResult>,
) {
    @JvmRecord
    data class AdminPromotionResult(
        val promotionId: Long?,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    )
}