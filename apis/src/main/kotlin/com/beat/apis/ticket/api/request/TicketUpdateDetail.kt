package com.beat.apis.ticket.api.request

import com.beat.apis.booking.api.type.BookingStatusType
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class TicketUpdateDetail(
    @field:NotNull val bookingId: Long?,
    val bookerName: String?,
    val bookerPhoneNumber: String?,
    val scheduleId: Long?,
    val purchaseTicketCount: Int?,
    val createdAt: LocalDateTime?,
    @field:NotNull val bookingStatus: BookingStatusType?,
    val scheduleNumber: String?,
)
