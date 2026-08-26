package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.BookingPerformanceScheduleResult
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "예매용 공연 상세정보의 회차별 좌석 및 예매 상태")
@ConsistentCopyVisibility
data class BookingPerformanceDetailScheduleResponse
private constructor(
    @field:Schema(
        description = "공연 회차 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val scheduleId: Long?,
    @field:Schema(
        description = "공연 시작 일시(ISO-8601)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026-09-01T19:00:00",
    )
    val performanceDate: LocalDateTime?,
    @field:Schema(
        description = "회차 번호 문자열",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "FIRST",
    )
    val scheduleNumber: String?,
    @field:Schema(
        description = "예매 가능한 잔여 티켓 수량",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "80",
    )
    val availableTicketCount: Int,
    @field:Schema(
        description = "현재 시각 기준 해당 회차의 예매 가능 여부",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "true",
    )
    @get:JsonProperty("isBooking")
    val isBooking: Boolean,
    @field:Schema(
        description = "기준일에서 공연일까지 남은 일수이며, 과거 회차는 음수입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val dueDate: Int,
) {
    companion object {
        fun from(
            result: BookingPerformanceScheduleResult
        ): BookingPerformanceDetailScheduleResponse =
            BookingPerformanceDetailScheduleResponse(
                scheduleId = result.scheduleId,
                performanceDate = result.performanceDate,
                scheduleNumber = result.scheduleNumber,
                availableTicketCount = result.availableTicketCount,
                isBooking = result.isBooking,
                dueDate = result.dueDate,
            )
    }
}
