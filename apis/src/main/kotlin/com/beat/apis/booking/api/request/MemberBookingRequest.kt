package com.beat.apis.booking.api.request
import com.beat.apis.booking.api.type.BookingStatusType

import com.beat.apis.schedule.api.type.ScheduleNumberType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

data class MemberBookingRequest(
    @field:NotNull @field:Positive val scheduleId: Long?,
    val scheduleNumber: ScheduleNumberType?,
    @field:NotNull @field:Positive val purchaseTicketCount: Int?,
    @field:NotBlank @field:Pattern(regexp = "^[a-zA-Z가-힣]+$") val bookerName: String?,
    @field:NotBlank @field:Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$") val bookerPhoneNumber: String?,
    val bookingStatus: BookingStatusType?,
    @field:NotNull @field:PositiveOrZero val totalPaymentAmount: Int?,
)
