package com.beat.apis.booking.api.request

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import jakarta.validation.constraints.NotNull

data class MemberBookingRequest(
    @field:NotNull val scheduleId: Long?,
    val scheduleNumber: ScheduleNumberType?,
    @field:NotNull val purchaseTicketCount: Int?,
    @field:NotNull val bookerName: String?,
    @field:NotNull val bookerPhoneNumber: String?,
    val bookingStatus: BookingStatusType?,
    val totalPaymentAmount: Int?,
)
