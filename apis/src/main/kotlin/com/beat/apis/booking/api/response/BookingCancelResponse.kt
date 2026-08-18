package com.beat.apis.booking.api.response

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.application.frontoffice.booking.result.BookingCancelResult

@ConsistentCopyVisibility
data class BookingCancelResponse private constructor(
    val bookingId: Long,
    val bookingStatus: BookingStatusType?,
) {
    companion object {
        fun from(result: BookingCancelResult): BookingCancelResponse = BookingCancelResponse(
            bookingId = result.bookingId,
            bookingStatus = result.bookingStatus?.let(BookingStatusType::valueOf),
        )
    }
}
