package com.beat.apis.file.api.response

@ConsistentCopyVisibility
data class PerformanceMakerPresignedUrlFindAllResponse private constructor(
    val performanceMakerPresignedUrls: Map<String, Map<String, String>>,
) {
    companion object {
        fun from(
            performanceMakerPresignedUrls: Map<String, Map<String, String>>,
        ): PerformanceMakerPresignedUrlFindAllResponse =
            PerformanceMakerPresignedUrlFindAllResponse(performanceMakerPresignedUrls)
    }
}
