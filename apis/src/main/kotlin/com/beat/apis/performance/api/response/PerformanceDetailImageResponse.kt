package com.beat.apis.performance.api.response

import com.beat.apis.performance.application.result.PerformanceImageResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceDetailImageResponse private constructor(
    val performanceImageId: Long?, @field:CdnImageUrl val performanceImage: String?,
) {
    companion object {
        fun from(result: PerformanceImageResult): PerformanceDetailImageResponse =
            PerformanceDetailImageResponse(result.id, result.image)
    }
}
