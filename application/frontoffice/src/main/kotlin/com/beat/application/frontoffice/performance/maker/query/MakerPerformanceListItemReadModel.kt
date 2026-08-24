package com.beat.application.frontoffice.performance.maker.query

import com.beat.application.frontoffice.query.PresentationReadModel
import java.time.LocalDate
import java.time.LocalDateTime

@PresentationReadModel
data class MakerPerformanceListItemReadModel(
    val performanceId: Long,
    val genre: String,
    val performanceTitle: String,
    val posterImage: String,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val representativePerformanceDate: LocalDateTime?,
)
