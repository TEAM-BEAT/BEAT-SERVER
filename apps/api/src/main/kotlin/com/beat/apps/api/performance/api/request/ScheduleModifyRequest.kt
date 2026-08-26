package com.beat.apps.api.performance.api.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공연 수정 시 공연 회차 한 건의 정보")
data class ScheduleModifyRequest(
    @field:Schema(
        description = "기존 회차의 식별자이며, 신규 회차를 추가할 때는 null입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1",
    )
    val scheduleId: Long?,
    @field:Schema(
        description = "공연 시작 일시(ISO-8601)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026-09-01T19:00:00",
    )
    val performanceDate: LocalDateTime,
    @field:Schema(
        description = "해당 회차의 전체 티켓 수량",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "100",
    )
    val totalTicketCount: Int,
)
