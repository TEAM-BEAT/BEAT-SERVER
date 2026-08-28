package com.beat.application.frontoffice.performance.booker.query

import com.beat.application.frontoffice.query.PresentationReadModel
import java.time.LocalDateTime

@PresentationReadModel
fun interface PerformanceScheduleAvailabilityReader {

    fun findAllByPerformanceId(performanceId: Long): List<PerformanceScheduleAvailabilityReadModel>
}

@PresentationReadModel
data class PerformanceScheduleAvailabilityReadModel(
    val scheduleId: Long,
    val performanceDate: LocalDateTime,
    val scheduleNumber: String,
    val availableTicketCount: Int,
    val isBooking: Boolean,
    val evaluatedAt: LocalDateTime,
)
