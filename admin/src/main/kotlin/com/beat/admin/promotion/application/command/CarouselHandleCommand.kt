package com.beat.admin.promotion.application.command

@JvmRecord
data class CarouselHandleCommand(
    val carousels: List<PromotionHandleCommand>,
) {
    @JvmRecord
    data class PromotionModifyCommand(
        val promotionId: Long,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleCommand

    @JvmRecord
    data class PromotionGenerateCommand(
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleCommand
}

sealed interface PromotionHandleCommand
