package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.result.BookingCancelResult
import com.beat.apps.api.booking.api.type.BookingStatusType

@ConsistentCopyVisibility
data class BookingCancelResponse
private constructor(
    val bookingId: Long,
    val bookingStatus: BookingStatusType?,
) {
    companion object {
        fun from(result: BookingCancelResult): BookingCancelResponse =
            BookingCancelResponse(
                bookingId = result.bookingId,
                bookingStatus = result.bookingStatus?.let(BookingStatusType::valueOf),
            )
    }
}
