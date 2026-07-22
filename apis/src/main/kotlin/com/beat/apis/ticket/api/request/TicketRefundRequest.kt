package com.beat.apis.ticket.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class TicketRefundRequest(
    @field:NotNull @field:Positive val performanceId: Long?,
    @field:NotNull @field:Size(min = 1) @field:Valid
    val bookingList: List<@NotNull @Valid Booking>?,
) {
    data class Booking(
        @field:NotNull @field:Positive val bookingId: Long?,
    )
}
