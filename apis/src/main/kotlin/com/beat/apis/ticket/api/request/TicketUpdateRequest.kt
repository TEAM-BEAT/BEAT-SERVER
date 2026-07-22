package com.beat.apis.ticket.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class TicketUpdateRequest(
    @field:NotNull @field:Positive val performanceId: Long?,
    val performanceTitle: String?,
    @field:NotNull @field:PositiveOrZero val totalScheduleCount: Int?,
    @field:NotNull @field:Size(min = 1) @field:Valid
    val bookingList: List<@NotNull @Valid TicketUpdateDetail>?,
)
