package com.beat.apis.file.api.response

data class PerformanceMakerPresignedUrlFindAllResponse(
    val performanceMakerPresignedUrls: Map<String, Map<String, String>>,
)
