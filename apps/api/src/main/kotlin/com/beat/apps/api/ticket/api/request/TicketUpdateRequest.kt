package com.beat.apps.api.ticket.api.request

import jakarta.validation.Valid

data class TicketUpdateRequest(
    val performanceId: Long,
    val performanceTitle: String?,
    val totalScheduleCount: Int?,
    @field:Valid val bookingList: List<@Valid TicketUpdateDetail>,
)
