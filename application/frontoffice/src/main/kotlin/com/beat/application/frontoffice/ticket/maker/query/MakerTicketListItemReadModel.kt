package com.beat.application.frontoffice.ticket.maker.query

import com.beat.application.frontoffice.query.PresentationReadModel
import java.time.LocalDateTime

@PresentationReadModel
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
