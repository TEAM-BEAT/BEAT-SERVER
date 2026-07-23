package com.beat.apis.ticket.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class TicketRefundRequest(
    @field:NotNull val performanceId: Long?,
    @field:NotNull @field:Valid val bookingList: List<@Valid Booking>?,
) {
    data class Booking(
        @field:NotNull val bookingId: Long?,
    )
}
