package com.beat.contracts.notification

import java.time.LocalDateTime

data class BookingNotification(
    val bookingDateTime: LocalDateTime,
    val performanceTitle: String,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val scheduleDisplayName: String,
    val currentSoldTicketCount: Int,
    val totalTicketCount: Int,
)
