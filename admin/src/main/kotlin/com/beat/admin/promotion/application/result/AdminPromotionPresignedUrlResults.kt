package com.beat.admin.promotion.application.result

import com.beat.contracts.storage.CarouselPresignedUpload

object AdminPromotionPresignedUrlResults {

    @JvmRecord
    data class CarouselPresignedUrlsResult(
        val carouselPresignedUploads: Map<String, CarouselPresignedUpload>,
    )

    @JvmRecord
    data class BannerPresignedUrlResult(
        val bannerPresignedUrl: String,
        val bannerImageKey: String,
    )
}