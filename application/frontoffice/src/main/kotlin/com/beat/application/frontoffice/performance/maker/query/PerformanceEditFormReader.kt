package com.beat.application.frontoffice.performance.maker.query

fun interface PerformanceEditFormReader {

    fun findByPerformanceId(performanceId: Long): PerformanceEditFormReadModel?
}
