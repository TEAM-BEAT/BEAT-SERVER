package com.beat.apps.api.ticket.api.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "공연의 예매자를 삭제하기 위한 요청입니다.")
data class TicketDeleteRequest(
    @field:Schema(
        description = "삭제할 공연 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "100",
    )
    val performanceId: Long,
    @field:Schema(
        description = "삭제할 예매 식별자 목록입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[{\"bookingId\":1}]",
    )
    @field:Valid
    val bookingList: List<@Valid Booking>,
) {
    @Schema(
        name = "TicketDeleteBookingReference",
        description = "삭제할 예매 식별자를 담는 객체입니다.",
    )
    data class Booking(
        @field:Schema(
            description = "삭제할 예매 식별자입니다.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1",
        )
        val bookingId: Long
    )
}
