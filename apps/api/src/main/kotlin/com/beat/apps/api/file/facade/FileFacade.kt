package com.beat.apps.api.file.facade

import com.beat.apps.api.file.api.response.PerformanceMakerPresignedUrlFindAllResponse
import com.beat.application.frontoffice.performance.maker.command.FileCommandService
import org.springframework.stereotype.Service

@Service
class FileFacade(
    private val fileCommandService: FileCommandService,
) {
    fun issueAllPresignedUrlsForPerformanceMaker(
        posterImage: String,
        castImages: List<String>?,
        staffImages: List<String>?,
        performanceImages: List<String>?,
    ): PerformanceMakerPresignedUrlFindAllResponse = PerformanceMakerPresignedUrlFindAllResponse.from(
        performanceMakerPresignedUploads = fileCommandService.issueAllPresignedUrlsForPerformanceMaker(
            posterImage,
            castImages,
            staffImages,
            performanceImages,
        ).performanceMakerPresignedUploads,
    )
}
