package com.beat.apis.ticket.api.request

import com.beat.apis.booking.api.type.BookingStatusType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class TicketUpdateDetail(
    @field:NotNull @field:Positive val bookingId: Long?,
    val bookerName: String?,
    val bookerPhoneNumber: String?,
    val scheduleId: Long?,
    @field:NotNull @field:Positive val purchaseTicketCount: Int?,
    val createdAt: LocalDateTime?,
    @field:NotNull val bookingStatus: BookingStatusType?,
    val scheduleNumber: String?,
)
