package com.beat.apis.booking.api.request

import com.beat.apis.performance.api.type.BankNameType

data class BookingRefundRequest(
    val bookingId: Long,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
)
