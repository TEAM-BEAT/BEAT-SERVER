package com.beat.application.frontoffice.performance.booker.query

import java.time.LocalDateTime

data class PerformanceScheduleAvailabilityReadModel(
    val scheduleId: Long,
    val performanceDate: LocalDateTime,
    val scheduleNumber: String,
    val availableTicketCount: Int,
    val isBooking: Boolean,
    val evaluatedAt: LocalDateTime,
)
