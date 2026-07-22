package com.beat.apis.home.api.response

import com.beat.apis.home.application.result.HomePromotionResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class HomePromotionDetail private constructor(
    val promotionId: Long?,
    @field:CdnImageUrl val promotionPhoto: String?,
    val performanceId: Long?,
    val redirectUrl: String?,
    val isExternal: Boolean,
    val carouselNumber: String?,
) {
    companion object {
        fun from(result: HomePromotionResult): HomePromotionDetail = HomePromotionDetail(
            promotionId = result.promotionId,
            promotionPhoto = result.promotionPhoto,
            performanceId = result.performanceId,
            redirectUrl = result.redirectUrl,
            isExternal = result.isExternal,
            carouselNumber = result.carouselNumber,
        )
    }
}
