package com.beat.contracts.storage

data class PerformancePresignedUrls(
    val performanceMakerPresignedUrls: Map<String, Map<String, String>>,
)
