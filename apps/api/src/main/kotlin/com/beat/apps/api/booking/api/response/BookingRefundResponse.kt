package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.result.BookingRefundResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.performance.api.type.BankNameType

@ConsistentCopyVisibility
data class BookingRefundResponse
private constructor(
    val bookingId: Long,
    val bookingStatus: BookingStatusType?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
) {
    companion object {
        fun from(result: BookingRefundResult): BookingRefundResponse =
            BookingRefundResponse(
                bookingId = result.bookingId,
                bookingStatus = result.bookingStatus?.let(BookingStatusType::valueOf),
                bankName = result.bankName?.let(BankNameType::valueOf),
                accountNumber = result.accountNumber,
                accountHolder = result.accountHolder,
            )
    }
}
