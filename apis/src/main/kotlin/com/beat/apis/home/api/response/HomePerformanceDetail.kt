package com.beat.apis.home.api.response

import com.beat.apis.home.application.result.HomePerformanceResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class HomePerformanceDetail private constructor(
    val performanceId: Long?,
    val performanceTitle: String?,
    val performancePeriod: String?,
    val ticketPrice: Int,
    val dueDate: Int,
    val genre: String?,
    @field:CdnImageUrl val posterImage: String?,
    val performanceVenue: String?,
) {
    companion object {
        fun from(result: HomePerformanceResult): HomePerformanceDetail = HomePerformanceDetail(
            performanceId = result.performanceId,
            performanceTitle = result.performanceTitle,
            performancePeriod = result.performancePeriod,
            ticketPrice = result.ticketPrice,
            dueDate = result.dueDate,
            genre = result.genre,
            posterImage = result.posterImage,
            performanceVenue = result.performanceVenue,
        )
    }
}
