package com.beat.apps.api.ticket.api.request

import com.beat.apps.api.booking.api.type.BookingStatusType
import java.time.LocalDateTime

data class TicketUpdateDetail(
    val bookingId: Long,
    val bookerName: String?,
    val bookerPhoneNumber: String?,
    val scheduleId: Long?,
    val purchaseTicketCount: Int?,
    val createdAt: LocalDateTime?,
    val bookingStatus: BookingStatusType,
    val scheduleNumber: String?,
)
