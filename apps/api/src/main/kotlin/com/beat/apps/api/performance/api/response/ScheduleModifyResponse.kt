package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.maker.command.result.ScheduleResult
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공연 수정 결과의 회차 정보")
@ConsistentCopyVisibility
data class ScheduleModifyResponse
private constructor(
    @field:Schema(
        description = "공연 회차 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val scheduleId: Long,
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
        description = "기준일에서 공연일까지 남은 일수이며, 과거 회차는 음수입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val dueDate: Int,
    @field:Schema(
        description = "회차 번호",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "FIRST",
    )
    val scheduleNumber: ScheduleNumberType,
) {
    companion object {
        fun from(result: ScheduleResult): ScheduleModifyResponse =
            ScheduleModifyResponse(
                scheduleId =
                    requireNotNull(result.id) {
                        "ScheduleModifyResponse.scheduleId must be present for a persisted schedule"
                    },
                performanceDate =
                    requireNotNull(result.performanceDate) {
                        "ScheduleModifyResponse.performanceDate must be present"
                    },
                totalTicketCount = result.totalTicketCount,
                dueDate = result.dueDate,
                scheduleNumber =
                    ScheduleNumberType.valueOf(
                        requireNotNull(result.scheduleNumber) {
                            "ScheduleModifyResponse.scheduleNumber must be present"
                        }
                    ),
            )
    }
}
