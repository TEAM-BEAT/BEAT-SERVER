package com.beat.contracts.storage

data class PerformancePresignedUrls(
    val performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>,
)
