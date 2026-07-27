package com.beat.apis.performance.api.response

import com.beat.apis.performance.application.result.BookingPerformanceScheduleResult
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class BookingPerformanceDetailScheduleResponse private constructor(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val scheduleNumber: String?,
    val availableTicketCount: Int,
    @get:JsonProperty("isBooking")
    val isBooking: Boolean,
    val dueDate: Int,
) {
    companion object {
        fun from(result: BookingPerformanceScheduleResult): BookingPerformanceDetailScheduleResponse =
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
