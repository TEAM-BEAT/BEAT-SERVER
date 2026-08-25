package com.beat.apps.api.file.api.response

import com.beat.application.frontoffice.performance.maker.command.ImagePresignedUpload

@ConsistentCopyVisibility
data class PerformanceMakerPresignedUrlFindAllResponse
private constructor(
    val performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>
) {
    companion object {
        fun from(
            performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>
        ): PerformanceMakerPresignedUrlFindAllResponse =
            PerformanceMakerPresignedUrlFindAllResponse(performanceMakerPresignedUploads)
    }
}
