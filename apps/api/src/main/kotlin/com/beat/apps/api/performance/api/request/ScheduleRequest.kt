package com.beat.apps.api.performance.api.request

import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공연 생성 시 공연 회차 한 건의 정보")
data class ScheduleRequest(
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
    @field:Schema(
        description = "회차 번호",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "FIRST",
    )
    val scheduleNumber: ScheduleNumberType,
)
