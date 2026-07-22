package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class PerformanceDetailImageResponse(
    val performanceImageId: Long?, @field:CdnImageUrl val performanceImage: String?,
)
