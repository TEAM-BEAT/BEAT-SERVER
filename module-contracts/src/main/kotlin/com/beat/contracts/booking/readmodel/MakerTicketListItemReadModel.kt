package com.beat.contracts.booking.readmodel


import com.beat.contracts.common.ReadModel
import java.time.LocalDateTime

@ReadModel
data class MakerTicketListItemReadModel(
    val bookingId: Long,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val createdAt: LocalDateTime,
    val bookingStatus: MakerTicketBookingStatus,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)
