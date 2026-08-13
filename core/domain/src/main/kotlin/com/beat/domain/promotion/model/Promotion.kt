package com.beat.domain.promotion.model

import com.beat.domain.performance.model.Performance
import com.beat.domain.sharedkernel.model.AggregateRoot

class Promotion private constructor(
    private val promotionId: Id?,
    val promotionPhoto: String,
    private val linkedPerformanceId: Performance.Id?,
    val redirectUrl: String,
    val isExternal: Boolean,
    val carouselNumber: CarouselNumber,
) : AggregateRoot {
    fun getId(): Long? = promotionId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Promotion) return false
        return promotionId != null && promotionId == other.promotionId
    }

    override fun hashCode(): Int = promotionId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Promotion(id=${getId()})"

    fun getPerformanceId(): Long? = linkedPerformanceId?.value

    fun updatePromotionDetails(
        carouselNumber: CarouselNumber,
        newImageUrl: String,
        isExternal: Boolean,
        redirectUrl: String,
        performanceId: Long?,
    ): Promotion = Promotion(
        promotionId = promotionId,
        carouselNumber = carouselNumber,
        promotionPhoto = newImageUrl,
        isExternal = isExternal,
        redirectUrl = redirectUrl,
        linkedPerformanceId = Performance.Id.fromNullable(performanceId),
    )

    fun updateCarouselNumber(carouselNumber: CarouselNumber): Promotion = Promotion(
        promotionId = promotionId,
        promotionPhoto = promotionPhoto,
        linkedPerformanceId = linkedPerformanceId,
        redirectUrl = redirectUrl,
        isExternal = isExternal,
        carouselNumber = carouselNumber,
    )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic
            fun from(value: Long): Id = Id(value)

            @JvmStatic
            fun fromNullable(value: Long?): Id? = value?.let(::from)
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
            linkedPerformanceId = Performance.Id.fromNullable(performanceId),
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
            promotionId = Id.fromNullable(id),
            promotionPhoto = promotionPhoto,
            linkedPerformanceId = Performance.Id.fromNullable(performanceId),
            redirectUrl = redirectUrl,
            isExternal = isExternal,
            carouselNumber = carouselNumber,
        )
    }
}
