package com.beat.apis.performance.api.response

import java.time.LocalDateTime

data class BookingPerformanceDetailScheduleResponse(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val scheduleNumber: String?,
    val availableTicketCount: Int,
    val isBooking: Boolean,
    val dueDate: Int,
)
