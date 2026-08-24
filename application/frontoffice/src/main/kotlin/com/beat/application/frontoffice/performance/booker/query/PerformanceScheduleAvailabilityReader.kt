package com.beat.application.frontoffice.performance.booker.query

import com.beat.application.frontoffice.query.PresentationReadModel

@PresentationReadModel
fun interface PerformanceScheduleAvailabilityReader {

    fun findAllByPerformanceId(performanceId: Long): List<PerformanceScheduleAvailabilityReadModel>
}
