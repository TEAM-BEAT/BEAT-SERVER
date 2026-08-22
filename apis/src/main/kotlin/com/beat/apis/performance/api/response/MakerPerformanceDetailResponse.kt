package com.beat.apis.performance.api.response

import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceResult
import com.beat.apis.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class MakerPerformanceDetailResponse private constructor(
    val performanceId: Long?,
    val genre: String?,
    val performanceTitle: String?,
    @field:CdnImageUrl val posterImage: String?,
    val performancePeriod: String?,
    val minDueDate: Int,
) {
    companion object {
        fun from(result: MakerPerformanceResult): MakerPerformanceDetailResponse = MakerPerformanceDetailResponse(
            performanceId = result.performanceId,
            genre = result.genre,
            performanceTitle = result.performanceTitle,
            posterImage = result.posterImage,
            performancePeriod = result.performancePeriod,
            minDueDate = result.minDueDate,
        )
    }
}
