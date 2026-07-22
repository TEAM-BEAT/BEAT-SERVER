package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class MakerPerformanceDetailResponse(
    val performanceId: Long?,
    val genre: String?,
    val performanceTitle: String?,
    @field:CdnImageUrl val posterImage: String?,
    val performancePeriod: String?,
    val minDueDate: Int,
)
