package com.beat.apis.booking.api.request

import jakarta.validation.constraints.NotNull

data class GuestBookingRetrieveRequest(
    @field:NotNull val bookerName: String?,
    @field:NotNull val birthDate: String?,
    @field:NotNull val bookerPhoneNumber: String?,
    @field:NotNull val password: String?,
)
