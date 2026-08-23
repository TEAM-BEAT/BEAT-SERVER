package com.beat.apis.booking.api.request

import jakarta.validation.constraints.NotNull

data class GuestBookingRequest(
    @field:NotNull val scheduleId: Long?,
    @field:NotNull val purchaseTicketCount: Int?,
    @field:NotNull val bookerName: String?,
    @field:NotNull val bookerPhoneNumber: String?,
    @field:NotNull val birthDate: String?,
    @field:NotNull val password: String?,
)
