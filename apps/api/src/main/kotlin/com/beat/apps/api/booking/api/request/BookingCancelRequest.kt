package com.beat.apps.api.booking.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "무료 공연 또는 미입금 예매 취소 요청")
data class BookingCancelRequest(
    @field:Schema(
        description = "취소할 예매의 식별자입니다.",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingId: Long
)
