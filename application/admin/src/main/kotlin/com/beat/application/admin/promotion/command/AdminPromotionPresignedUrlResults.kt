package com.beat.application.admin.promotion.command

object AdminPromotionPresignedUrlResults {

    data class CarouselPresignedUrlsResult(
        val carouselPresignedUploads: Map<String, PromotionImageUpload>
    )

    data class BannerPresignedUrlResult(
        val bannerPresignedUrl: String,
        val bannerImageKey: String,
    )
}
