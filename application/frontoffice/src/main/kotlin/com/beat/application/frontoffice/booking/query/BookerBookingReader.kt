package com.beat.application.frontoffice.booking.query

import java.time.LocalDateTime

fun interface BookerBookingReader {
    fun findByUserId(userId: Long): List<BookerBookingReadModel>
}

data class BookerBookingReadModel(
    val userId: Long,
    val bookingId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookingStatus: String,
    val createdAt: LocalDateTime,
    val totalPaymentAmount: Int?,
    val schedule: BookerBookingScheduleReadModel?,
    val performance: BookerBookingPerformanceReadModel?,
)

data class BookerBookingScheduleReadModel(
    val scheduleId: Long,
    val performanceId: Long,
    val performanceDate: LocalDateTime,
    val scheduleNumber: String,
)

data class BookerBookingPerformanceReadModel(
    val performanceId: Long,
    val performanceTitle: String,
    val performanceVenue: String,
    val performanceContact: String,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val ticketPrice: Int,
)
