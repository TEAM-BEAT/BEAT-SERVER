package com.beat.contracts.promotion.readmodel

import com.beat.contracts.common.ReadModel

@ReadModel
data class HomePromotionReadModel(
    val promotionId: Long,
    val promotionPhoto: String,
    val performanceId: Long?,
    val redirectUrl: String,
    val external: Boolean,
    val carouselNumber: String,
)
