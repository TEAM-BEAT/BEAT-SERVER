package com.beat.application.admin.promotion.query

import com.beat.application.admin.promotion.PromotionImageUpload

object AdminPromotionPresignedUrlResults {

    data class CarouselPresignedUrlsResult(
        val carouselPresignedUploads: Map<String, PromotionImageUpload>,
    )

    data class BannerPresignedUrlResult(
        val bannerPresignedUrl: String,
        val bannerImageKey: String,
    )
}
