package com.beat.apps.api.ticket.api.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "메이커가 예매자의 입금 여부를 수정하기 위한 요청입니다.")
data class TicketUpdateRequest(
    @field:Schema(
        description = "입금 여부를 수정할 공연 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "100",
    )
    val performanceId: Long,
    @field:Schema(
        description = "공연 제목입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "title",
    )
    val performanceTitle: String?,
    @field:Schema(
        description = "공연의 전체 회차 수입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1",
    )
    val totalScheduleCount: Int?,
    @field:Schema(
        description = "입금 여부를 수정할 예매자 목록입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[{\"bookingId\":1,\"bookingStatus\":\"CHECKING_PAYMENT\"}]",
    )
    @field:Valid
    val bookingList: List<@Valid TicketUpdateDetail>,
)
