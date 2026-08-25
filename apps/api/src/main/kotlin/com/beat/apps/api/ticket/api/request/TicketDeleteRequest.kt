package com.beat.apps.api.ticket.api.request

import jakarta.validation.Valid

data class TicketDeleteRequest(
    val performanceId: Long,
    @field:Valid val bookingList: List<@Valid Booking>,
) {
    data class Booking(val bookingId: Long)
}
