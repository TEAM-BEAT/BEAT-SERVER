package com.beat.apps.api.ticket.api.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "환불 완료 처리할 공연의 예매자를 지정하는 요청입니다.")
data class TicketRefundRequest(
    @field:Schema(
        description = "환불 완료 처리할 공연 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "100",
    )
    val performanceId: Long,
    @field:Schema(
        description = "환불 완료 처리할 예매 식별자 목록입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[{\"bookingId\":1}]",
    )
    @field:Valid
    val bookingList: List<@Valid Booking>,
) {
    @Schema(
        name = "TicketRefundBookingReference",
        description = "환불 완료 처리할 예매 식별자를 담는 객체입니다.",
    )
    data class Booking(
        @field:Schema(
            description = "환불 완료 처리할 예매 식별자입니다.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1",
        )
        val bookingId: Long
    )
}
