package com.beat.apis.schedule.application.result

data class TicketAvailabilityResult(
    val scheduleId: Long?,
    val scheduleNumber: String?,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val availableTicketCount: Int,
    val requestedTicketCount: Int,
    val isAvailable: Boolean,
)
