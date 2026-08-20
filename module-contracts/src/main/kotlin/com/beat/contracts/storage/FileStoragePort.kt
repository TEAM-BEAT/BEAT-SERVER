package com.beat.contracts.storage


interface FileStoragePort {

    fun issueAllPresignedUrlsForCarousel(carouselImages: List<String>): CarouselPresignedUrls

    fun findImageObjectMetadata(imageKey: String): ImageObjectMetadata?

    fun issuePresignedUrlForBanner(bannerImage: String): BannerPresignedUrl
}
