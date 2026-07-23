package com.beat.apis.booking.api.request

import com.beat.apis.performance.api.type.BankNameType
import jakarta.validation.constraints.NotNull

data class BookingRefundRequest(
    @field:NotNull val bookingId: Long?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
)
