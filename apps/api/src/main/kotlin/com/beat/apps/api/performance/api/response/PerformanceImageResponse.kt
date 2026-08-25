package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.apps.api.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceImageResponse
private constructor(
    val imageId: Long?,
    @field:CdnImageUrl val imageUrl: String?,
) {
    companion object {
        fun from(result: PerformanceImageResult): PerformanceImageResponse =
            PerformanceImageResponse(result.id, result.image)
    }
}
