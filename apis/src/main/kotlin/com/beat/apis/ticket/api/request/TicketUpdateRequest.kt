package com.beat.apis.ticket.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class TicketUpdateRequest(
    @field:NotNull val performanceId: Long?,
    val performanceTitle: String?,
    val totalScheduleCount: Int?,
    @field:NotNull @field:Valid val bookingList: List<@Valid TicketUpdateDetail>?,
)
