package com.beat.apis.booking.api.request

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import jakarta.validation.constraints.NotNull

data class GuestBookingRequest(
    @field:NotNull val scheduleId: Long?,
    @field:NotNull val purchaseTicketCount: Int?,
    val scheduleNumber: ScheduleNumberType?,
    @field:NotNull val bookerName: String?,
    @field:NotNull val bookerPhoneNumber: String?,
    @field:NotNull val birthDate: String?,
    @field:NotNull val password: String?,
    val totalPaymentAmount: Int?,
    val bookingStatus: BookingStatusType?,
)
