package com.beat.apps.api.home.api.response

import com.beat.application.frontoffice.home.booker.query.HomePerformanceResult
import com.beat.apps.api.web.jackson.CdnImageUrl

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
