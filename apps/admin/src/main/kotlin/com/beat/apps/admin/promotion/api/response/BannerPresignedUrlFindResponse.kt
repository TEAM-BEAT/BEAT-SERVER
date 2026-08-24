package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult

data class BannerPresignedUrlFindResponse(
    val bannerPresignedUrl: String,
    val bannerPresignedUpload: BannerPresignedUploadResponse,
) {
    constructor(result: BannerPresignedUrlResult) : this(
        bannerPresignedUrl = result.bannerPresignedUrl,
        bannerPresignedUpload = BannerPresignedUploadResponse(result.bannerPresignedUrl, result.bannerImageKey),
    )

    data class BannerPresignedUploadResponse(
        val uploadUrl: String,
        val imageKey: String,
    )
}
