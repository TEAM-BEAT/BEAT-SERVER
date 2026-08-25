package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.PromotionImageUpload
import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult

data class CarouselPresignedUrlFindAllResponse(
    val carouselPresignedUrls: Map<String, String>,
    val carouselPresignedUploads: Map<String, CarouselPresignedUploadResponse>,
) {
    constructor(
        result: CarouselPresignedUrlsResult
    ) : this(
        carouselPresignedUrls = result.carouselPresignedUploads.mapValues { it.value.uploadUrl },
        carouselPresignedUploads =
            result.carouselPresignedUploads.mapValues {
                CarouselPresignedUploadResponse(it.value)
            },
    )

    data class CarouselPresignedUploadResponse(
        val uploadUrl: String,
        val imageKey: String,
    ) {
        constructor(upload: PromotionImageUpload) : this(upload.uploadUrl, upload.imageKey)
    }
}
