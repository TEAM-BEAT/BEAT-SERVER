package com.beat.apis.file.api.response

import com.beat.contracts.storage.ImagePresignedUpload

@ConsistentCopyVisibility
data class PerformanceMakerPresignedUrlFindAllResponse private constructor(
    val performanceMakerPresignedUrls: Map<String, Map<String, String>>,
    val performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>,
) {
    companion object {
        fun from(
            performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>,
        ): PerformanceMakerPresignedUrlFindAllResponse {
            val legacyUrls = performanceMakerPresignedUploads.mapValues { (_, uploads) ->
                uploads.mapValues { (_, upload) -> upload.uploadUrl }
            }
            return PerformanceMakerPresignedUrlFindAllResponse(legacyUrls, performanceMakerPresignedUploads)
        }
    }
}
