package com.beat.apis.performance.api.response

import com.beat.apis.performance.application.result.PerformanceImageResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceImageModifyResponse private constructor(
    val performanceImageId: Long?, @field:CdnImageUrl val performanceImage: String?,
) {
    companion object {
        fun from(result: PerformanceImageResult): PerformanceImageModifyResponse =
            PerformanceImageModifyResponse(result.id, result.image)
    }
}
