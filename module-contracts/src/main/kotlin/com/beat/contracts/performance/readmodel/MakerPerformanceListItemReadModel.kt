package com.beat.contracts.performance.readmodel

import com.beat.contracts.common.ReadModel
import java.time.LocalDate
import java.time.LocalDateTime

@ReadModel
data class MakerPerformanceListItemReadModel(
    val performanceId: Long,
    val genre: String,
    val performanceTitle: String,
    val posterImage: String,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val representativePerformanceDate: LocalDateTime?,
)
