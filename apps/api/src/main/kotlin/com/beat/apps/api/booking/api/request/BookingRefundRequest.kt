package com.beat.apps.api.booking.api.request

import com.beat.apps.api.performance.api.type.BankNameType

data class BookingRefundRequest(
    val bookingId: Long,
    val bankName: BankNameType,
    val accountNumber: String,
    val accountHolder: String,
)
