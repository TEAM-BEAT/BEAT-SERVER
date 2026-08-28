package com.beat.application.frontoffice.booking.booker.query.result

import java.time.LocalDateTime

data class BookingRetrieveResult(
    val userId: Long,
    val bookingId: Long,
    val scheduleId: Long,
    val performanceId: Long,
    val performanceTitle: String,
    val performanceDate: LocalDateTime,
    val performanceVenue: String,
    val purchaseTicketCount: Int,
    val scheduleNumber: String,
    val bookerName: String,
    val performanceContact: String,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
    val dueDate: Int,
    val bookingStatus: String,
    val createdAt: LocalDateTime,
    val posterImage: String,
    val totalPaymentAmount: Int,
)
