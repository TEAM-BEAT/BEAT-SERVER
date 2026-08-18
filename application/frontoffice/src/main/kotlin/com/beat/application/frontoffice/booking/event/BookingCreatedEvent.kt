package com.beat.application.frontoffice.booking.event

import java.time.LocalDateTime

data class BookingCreatedEvent(
    val bookingDateTime: LocalDateTime,
    val performanceTitle: String,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val scheduleDisplayName: String,
    val currentSoldTicketCount: Int,
    val totalTicketCount: Int,
)
