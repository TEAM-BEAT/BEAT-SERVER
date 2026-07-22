package com.beat.apis.booking.api.request

import com.beat.apis.performance.api.type.BankNameType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class BookingRefundRequest(
    @field:NotNull @field:Positive val bookingId: Long?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
)
