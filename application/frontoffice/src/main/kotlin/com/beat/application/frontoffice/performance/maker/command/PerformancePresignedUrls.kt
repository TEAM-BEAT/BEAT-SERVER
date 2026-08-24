package com.beat.application.frontoffice.performance.maker.command

data class PerformancePresignedUrls(
    val performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>,
)
