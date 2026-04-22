package com.beat.domain.promotion.domain

import com.beat.domain.performance.domain.PerformanceId
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class Promotion private constructor(
    private val promotionId: Id?,
    val promotionPhoto: String,
    private val linkedPerformanceId: PerformanceId?,
    val redirectUrl: String,
    val isExternal: Boolean,
    val carouselNumber: CarouselNumber,
) {
    fun getId(): Long? = promotionId?.value

    fun getPerformanceId(): Long? = linkedPerformanceId?.value

    fun updatePromotionDetails(
        carouselNumber: CarouselNumber,
        newImageUrl: String,
        isExternal: Boolean,
        redirectUrl: String,
        performanceId: Long?,
    ): Promotion = copy(
        carouselNumber = carouselNumber,
        promotionPhoto = newImageUrl,
        isExternal = isExternal,
        redirectUrl = redirectUrl,
        linkedPerformanceId = PerformanceId.of(performanceId),
    )

    fun updateCarouselNumber(carouselNumber: CarouselNumber): Promotion = copy(carouselNumber = carouselNumber)

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun of(value: Long?): Id? = value?.let(::Id)
        }
    }

    companion object {
        @JvmStatic
        fun create(
            promotionPhoto: String,
            performanceId: Long?,
            redirectUrl: String,
            isExternal: Boolean,
            carouselNumber: CarouselNumber,
        ): Promotion = Promotion(
            promotionId = null,
            promotionPhoto = promotionPhoto,
            linkedPerformanceId = PerformanceId.of(performanceId),
            redirectUrl = redirectUrl,
            isExternal = isExternal,
            carouselNumber = carouselNumber,
        )

        @JvmStatic
        fun rehydrate(
            id: Long?,
            promotionPhoto: String,
            performanceId: Long?,
            redirectUrl: String,
            isExternal: Boolean,
            carouselNumber: CarouselNumber,
        ): Promotion = Promotion(
            promotionId = Id.of(id),
            promotionPhoto = promotionPhoto,
            linkedPerformanceId = PerformanceId.of(performanceId),
            redirectUrl = redirectUrl,
            isExternal = isExternal,
            carouselNumber = carouselNumber,
        )
    }
}
