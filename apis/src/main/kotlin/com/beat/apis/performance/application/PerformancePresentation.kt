package com.beat.apis.performance.application

import com.beat.apis.schedule.application.calculateDueDate
import com.beat.domain.performance.vo.PerformancePeriod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val performanceDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

internal fun formatPerformancePeriod(period: PerformancePeriod): String {
    val start = period.startDate.format(performanceDateFormatter)
    return if (period.startDate == period.endDate) {
        start
    } else {
        "$start~${period.endDate.format(performanceDateFormatter)}"
    }
}

internal fun nearestDueDate(today: LocalDate, performanceDates: List<LocalDateTime>): Int {
    val dueDates = performanceDates.map { calculateDueDate(today, it) }
    return dueDates.filter { it >= 0 }.minOrNull() ?: dueDates.minOrNull() ?: Int.MAX_VALUE
}
