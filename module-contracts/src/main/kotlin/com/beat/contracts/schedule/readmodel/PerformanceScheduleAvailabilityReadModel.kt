package com.beat.contracts.schedule.readmodel

import com.beat.contracts.common.ReadModel
import java.time.LocalDateTime

@ReadModel
data class PerformanceScheduleAvailabilityReadModel(
    val scheduleId: Long,
    val performanceDate: LocalDateTime,
    val scheduleNumber: String,
    val availableTicketCount: Int,
    val isBooking: Boolean,
    val evaluatedAt: LocalDateTime,
)
