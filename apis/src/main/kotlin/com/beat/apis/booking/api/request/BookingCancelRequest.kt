package com.beat.apis.booking.api.request

import jakarta.validation.constraints.NotNull

data class BookingCancelRequest(
    @field:NotNull val bookingId: Long?,
)
