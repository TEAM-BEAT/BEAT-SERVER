package com.beat.apis.performance.api.response

import java.time.LocalDateTime

data class PerformanceDetailScheduleResponse(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val scheduleNumber: String?,
    val dueDate: Int,
    val isBooking: Boolean,
)
