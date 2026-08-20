package com.beat.application.frontoffice.performance.booker.query

fun interface PerformanceScheduleAvailabilityReader {

    fun findAllByPerformanceId(performanceId: Long): List<PerformanceScheduleAvailabilityReadModel>
}
