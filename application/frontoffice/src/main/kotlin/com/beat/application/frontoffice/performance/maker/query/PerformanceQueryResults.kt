package com.beat.application.frontoffice.performance.maker.query

import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult

data class PerformanceEditResult(
    val performance: PerformanceMutationResult,
    val isBookerExist: Boolean,
)

data class MakerPerformanceListResult(
    val userId: Long?,
    val performances: List<MakerPerformanceResult>,
)

data class MakerPerformanceResult(
    val performanceId: Long?,
    val genre: String?,
    val performanceTitle: String?,
    val posterImage: String?,
    val performancePeriod: String?,
    val minDueDate: Int,
)
