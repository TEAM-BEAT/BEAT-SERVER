package com.beat.apis.performance.api.response

import com.beat.apis.performance.application.result.PerformanceDetailScheduleResult
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class PerformanceDetailScheduleResponse private constructor(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val scheduleNumber: String?,
    val dueDate: Int,
    @get:JsonProperty("isBooking")
    val isBooking: Boolean,
) {
    companion object {
        fun from(result: PerformanceDetailScheduleResult): PerformanceDetailScheduleResponse =
            PerformanceDetailScheduleResponse(
                result.scheduleId, result.performanceDate, result.scheduleNumber, result.dueDate, result.isBooking,
            )
    }
}
