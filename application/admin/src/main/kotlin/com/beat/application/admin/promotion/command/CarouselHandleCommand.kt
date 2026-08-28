package com.beat.application.admin.promotion.command

data class CarouselHandleCommand(val carousels: List<PromotionHandleCommand>) {
    data class PromotionModifyCommand(
        val promotionId: Long,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleCommand

    data class PromotionGenerateCommand(
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleCommand
}

sealed interface PromotionHandleCommand
