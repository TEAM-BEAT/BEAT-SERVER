package com.beat.apis.booking.api.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class BookingCancelRequest(
    @field:NotNull @field:Positive val bookingId: Long?,
)
