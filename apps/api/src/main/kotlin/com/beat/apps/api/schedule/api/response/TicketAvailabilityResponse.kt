package com.beat.apps.api.schedule.api.response

import com.beat.application.frontoffice.schedule.booker.query.TicketAvailabilityResult
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회차의 티켓 재고와 요청 수량에 따른 구매 가능 여부입니다.")
@ConsistentCopyVisibility
data class TicketAvailabilityResponse
private constructor(
    @field:Schema(
        description = "회차 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val scheduleId: Long?,
    @field:Schema(
        description = "회차 번호입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1회차",
    )
    val scheduleNumber: String?,
    @field:Schema(
        description = "회차에 배정된 전체 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "10",
    )
    val totalTicketCount: Int,
    @field:Schema(
        description = "회차에서 판매된 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2",
    )
    val soldTicketCount: Int,
    @field:Schema(
        description = "회차에서 구매할 수 있는 잔여 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "8",
    )
    val availableTicketCount: Int,
    @field:Schema(
        description = "구매 가능 여부를 확인하기 위해 요청한 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val requestedTicketCount: Int,
    @field:Schema(
        description = "요청한 티켓 수량만큼 구매할 수 있는지 여부입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "true",
    )
    @get:JsonProperty("isAvailable")
    val isAvailable: Boolean,
) {
    companion object {
        fun from(result: TicketAvailabilityResult): TicketAvailabilityResponse =
            TicketAvailabilityResponse(
                scheduleId = result.scheduleId,
                scheduleNumber = result.scheduleNumber,
                totalTicketCount = result.totalTicketCount,
                soldTicketCount = result.soldTicketCount,
                availableTicketCount = result.availableTicketCount,
                requestedTicketCount = result.requestedTicketCount,
                isAvailable = result.isAvailable,
            )
    }
}
