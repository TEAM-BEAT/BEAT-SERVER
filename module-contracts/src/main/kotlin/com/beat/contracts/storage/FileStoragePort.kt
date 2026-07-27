package com.beat.contracts.storage


interface FileStoragePort {

    fun issueAllPresignedUrlsForPerformanceMaker(
        posterImage: String,
        castImages: List<String>,
        staffImages: List<String>,
        performanceImages: List<String>,
    ): PerformancePresignedUrls

    fun issueAllPresignedUrlsForCarousel(carouselImages: List<String>): CarouselPresignedUrls

    fun findCarouselImageObjectMetadata(imageKey: String): ImageObjectMetadata?

    fun issuePresignedUrlForBanner(bannerImage: String): BannerPresignedUrl
}
