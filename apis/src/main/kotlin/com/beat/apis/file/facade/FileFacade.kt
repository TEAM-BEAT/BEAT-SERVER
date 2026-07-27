package com.beat.apis.file.facade

import com.beat.apis.file.api.response.PerformanceMakerPresignedUrlFindAllResponse
import com.beat.apis.file.application.command.FileCommandService
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
