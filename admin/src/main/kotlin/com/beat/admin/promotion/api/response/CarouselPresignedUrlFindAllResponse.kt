package com.beat.admin.promotion.api.response

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.contracts.storage.CarouselPresignedUpload

data class CarouselPresignedUrlFindAllResponse(
    val carouselPresignedUrls: Map<String, String>,
    val carouselPresignedUploads: Map<String, CarouselPresignedUploadResponse>,
) {
    constructor(result: CarouselPresignedUrlsResult) : this(
        carouselPresignedUrls = result.carouselPresignedUploads.mapValues { it.value.uploadUrl },
        carouselPresignedUploads = result.carouselPresignedUploads.mapValues {
            CarouselPresignedUploadResponse(it.value)
        },
    )

    data class CarouselPresignedUploadResponse(
        val uploadUrl: String,
        val imageKey: String,
    ) {
        constructor(upload: CarouselPresignedUpload) : this(upload.uploadUrl, upload.imageKey)
    }
}