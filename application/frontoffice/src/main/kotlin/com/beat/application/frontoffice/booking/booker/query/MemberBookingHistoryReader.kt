package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.query.PresentationReadModel
import java.time.LocalDateTime

@PresentationReadModel
fun interface MemberBookingHistoryReader {
    fun findByUserId(userId: Long): List<MemberBookingHistoryReadModel>
}

@PresentationReadModel
data class MemberBookingHistoryReadModel(
    val userId: Long,
    val bookingId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookingStatus: String,
    val createdAt: LocalDateTime,
    val totalPaymentAmount: Int?,
    val schedule: MemberBookingHistoryScheduleReadModel?,
    val performance: MemberBookingHistoryPerformanceReadModel?,
)

@PresentationReadModel
data class MemberBookingHistoryScheduleReadModel(
    val scheduleId: Long,
    val performanceId: Long,
    val performanceDate: LocalDateTime,
    val scheduleNumber: String,
)

@PresentationReadModel
data class MemberBookingHistoryPerformanceReadModel(
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
