package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.PerformanceDetailScheduleResult
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공연 상세정보의 회차별 예매 상태")
@ConsistentCopyVisibility
data class PerformanceDetailScheduleResponse
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
        description = "회차 번호 문자열",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "FIRST",
    )
    val scheduleNumber: String,
    @field:Schema(
        description = "기준일에서 공연일까지 남은 일수이며, 과거 회차는 음수입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val dueDate: Int,
    @field:Schema(
        description = "현재 시각 기준 해당 회차의 예매 가능 여부",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "true",
    )
    @get:JsonProperty("isBooking")
    val isBooking: Boolean,
) {
    companion object {
        fun from(result: PerformanceDetailScheduleResult): PerformanceDetailScheduleResponse =
            PerformanceDetailScheduleResponse(
                requireNotNull(result.scheduleId) {
                    "PerformanceDetailScheduleResponse.scheduleId must be present for a persisted schedule"
                },
                requireNotNull(result.performanceDate) {
                    "PerformanceDetailScheduleResponse.performanceDate must be present"
                },
                requireNotNull(result.scheduleNumber) {
                    "PerformanceDetailScheduleResponse.scheduleNumber must be present"
                },
                result.dueDate,
                result.isBooking,
            )
    }
}
