package com.beat.contracts.storage

data class CarouselPresignedUrls(
    val carouselPresignedUploads: Map<String, CarouselPresignedUpload>,
)
