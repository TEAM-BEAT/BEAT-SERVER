package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class BookingPerformanceDetailResponse(
    val performanceId: Long?,
    val performanceTitle: String?,
    val performancePeriod: String?,
    val scheduleList: List<BookingPerformanceDetailScheduleResponse>,
    val ticketPrice: Int,
    val genre: String?, @field:CdnImageUrl val posterImage: String?,
    val performanceVenue: String?,
    val performanceTeamName: String?,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
)
