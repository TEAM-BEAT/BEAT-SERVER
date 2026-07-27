package com.beat.apis.schedule.api.response

import com.beat.apis.schedule.application.result.TicketAvailabilityResult
import com.fasterxml.jackson.annotation.JsonProperty

@ConsistentCopyVisibility
data class TicketAvailabilityResponse private constructor(
    val scheduleId: Long?,
    val scheduleNumber: String?,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val availableTicketCount: Int,
    val requestedTicketCount: Int,
    @get:JsonProperty("isAvailable")
    val isAvailable: Boolean,
) {
    companion object {
        fun from(result: TicketAvailabilityResult): TicketAvailabilityResponse = TicketAvailabilityResponse(
            scheduleId = result.scheduleId,
            scheduleNumber = result.scheduleNumber,
            totalTicketCount = result.totalTicketCount,
            soldTicketCount = result.soldTicketCount,
            availableTicketCount = result.availableTicketCount,
            requestedTicketCount = result.requestedTicketCount,
            isAvailable = result.isAvailable,
        )
    }
}
