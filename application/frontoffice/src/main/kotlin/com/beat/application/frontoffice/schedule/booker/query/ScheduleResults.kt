package com.beat.application.frontoffice.schedule.booker.query

data class TicketAvailabilityResult(
    val scheduleId: Long?,
    val scheduleNumber: String?,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val availableTicketCount: Int,
    val requestedTicketCount: Int,
    val isAvailable: Boolean,
)
