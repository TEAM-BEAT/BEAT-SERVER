package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.BookingPerformanceDetailResult
import com.beat.apps.api.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class BookingPerformanceDetailResponse private constructor(
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
) {
    companion object {
        fun from(result: BookingPerformanceDetailResult): BookingPerformanceDetailResponse = BookingPerformanceDetailResponse(
            performanceId = result.performanceId,
            performanceTitle = result.performanceTitle,
            performancePeriod = result.performancePeriod,
            scheduleList = result.schedules.map(BookingPerformanceDetailScheduleResponse::from),
            ticketPrice = result.ticketPrice,
            genre = result.genre,
            posterImage = result.posterImage,
            performanceVenue = result.performanceVenue,
            performanceTeamName = result.performanceTeamName,
            bankName = result.bankName,
            accountNumber = result.accountNumber,
            accountHolder = result.accountHolder,
        )
    }
}
