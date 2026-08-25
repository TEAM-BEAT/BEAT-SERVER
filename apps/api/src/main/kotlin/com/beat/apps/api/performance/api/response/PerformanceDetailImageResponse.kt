package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.apps.api.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceDetailImageResponse
private constructor(
    val performanceImageId: Long?,
    @field:CdnImageUrl val performanceImage: String?,
) {
    companion object {
        fun from(result: PerformanceImageResult): PerformanceDetailImageResponse =
            PerformanceDetailImageResponse(result.id, result.image)
    }
}
