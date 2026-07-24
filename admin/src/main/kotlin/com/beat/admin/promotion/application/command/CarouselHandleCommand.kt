package com.beat.admin.promotion.application.command

@JvmRecord
@ConsistentCopyVisibility
data class CarouselHandleCommand private constructor(
    val carousels: List<PromotionHandleCommand>,
) {
    @JvmRecord
    @ConsistentCopyVisibility
    data class PromotionModifyCommand private constructor(
        val promotionId: Long,
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleCommand {
        companion object {
            @JvmStatic
            fun of(
                promotionId: Long,
                carouselNumber: String,
                newImageUrl: String,
                isExternal: Boolean,
                redirectUrl: String,
                performanceId: Long?,
            ): PromotionModifyCommand = PromotionModifyCommand(
                promotionId = promotionId,
                carouselNumber = carouselNumber,
                newImageUrl = newImageUrl,
                isExternal = isExternal,
                redirectUrl = redirectUrl,
                performanceId = performanceId,
            )
        }
    }

    @JvmRecord
    @ConsistentCopyVisibility
    data class PromotionGenerateCommand private constructor(
        val carouselNumber: String,
        val newImageUrl: String,
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleCommand {
        companion object {
            @JvmStatic
            fun of(
                carouselNumber: String,
                newImageUrl: String,
                isExternal: Boolean,
                redirectUrl: String,
                performanceId: Long?,
            ): PromotionGenerateCommand = PromotionGenerateCommand(
                carouselNumber = carouselNumber,
                newImageUrl = newImageUrl,
                isExternal = isExternal,
                redirectUrl = redirectUrl,
                performanceId = performanceId,
            )
        }
    }

    companion object {
        @JvmStatic
        fun from(carousels: List<PromotionHandleCommand>): CarouselHandleCommand = CarouselHandleCommand(carousels)
    }
}

sealed interface PromotionHandleCommand
