package com.beat.admin.promotion.api.response

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult

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