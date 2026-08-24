package com.beat.application.frontoffice.performance.maker.query

import com.beat.application.frontoffice.query.PresentationReadModel

@PresentationReadModel
fun interface PerformanceEditFormReader {

    fun findByPerformanceId(performanceId: Long): PerformanceEditFormReadModel?
}
