package com.beat.apis.ticket.api.response

import com.beat.apis.ticket.application.result.TicketRetrieveResult

@ConsistentCopyVisibility
data class TicketRetrieveResponse private constructor(
    val performanceTitle: String?,
    val performanceTeamName: String?,
    val ticketPrice: Int,
    val totalScheduleCount: Int,
    val totalPerformanceTicketCount: Int,
    val totalPerformanceSoldTicketCount: Int,
    val bookingList: List<TicketDetail>,
) {
    companion object {
        fun from(result: TicketRetrieveResult): TicketRetrieveResponse = TicketRetrieveResponse(
            performanceTitle = result.performanceTitle,
            performanceTeamName = result.performanceTeamName,
            ticketPrice = result.ticketPrice,
            totalScheduleCount = result.totalScheduleCount,
            totalPerformanceTicketCount = result.totalPerformanceTicketCount,
            totalPerformanceSoldTicketCount = result.totalPerformanceSoldTicketCount,
            bookingList = result.bookingList.map(TicketDetail::from),
        )
    }
}
