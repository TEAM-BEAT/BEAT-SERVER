package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.command.result.BookingCancelResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import io.swagger.v3.oas.annotations.media.Schema

@ConsistentCopyVisibility
@Schema(description = "예매 취소 처리 결과")
data class BookingCancelResponse
private constructor(
    @field:Schema(
        description = "취소 처리된 예매의 식별자입니다.",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingId: Long,
    @field:Schema(
        description = "취소 처리 후 예매 상태입니다.",
        example = "BOOKING_CANCELLED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingStatus: BookingStatusType,
) {
    companion object {
        fun from(result: BookingCancelResult): BookingCancelResponse =
            BookingCancelResponse(
                bookingId = result.bookingId,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
            )
    }
}
