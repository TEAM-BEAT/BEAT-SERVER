package com.beat.application.frontoffice.ticket.query

import java.time.LocalDateTime

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
    val deletable: Boolean,
)
