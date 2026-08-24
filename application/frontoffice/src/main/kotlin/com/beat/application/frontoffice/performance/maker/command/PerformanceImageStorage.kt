package com.beat.application.frontoffice.performance.maker.command

interface PerformanceImageStorage {
    fun issueAllPresignedUrls(
        posterImage: String,
        castImages: List<String>,
        staffImages: List<String>,
        performanceImages: List<String>,
    ): PerformancePresignedUrls

    fun exists(imageKey: String): Boolean
}
