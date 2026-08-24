package com.beat.application.frontoffice.booking.booker

import java.time.LocalDateTime

/** Output-only booking history projection; command correctness must not depend on these snapshots. */
fun interface BookingHistoryReadPort {
    fun findByUserId(userId: Long): List<BookingHistorySnapshot>
}

data class BookingHistorySnapshot(
    val userId: Long,
    val bookingId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookingStatus: String,
    val createdAt: LocalDateTime,
    val totalPaymentAmount: Int?,
    val schedule: BookingHistoryScheduleSnapshot?,
    val performance: BookingHistoryPerformanceSnapshot?,
)

data class BookingHistoryScheduleSnapshot(
    val scheduleId: Long,
    val performanceId: Long,
    val performanceDate: LocalDateTime,
    val scheduleNumber: String,
)

data class BookingHistoryPerformanceSnapshot(
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
