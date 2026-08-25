package com.beat.domain.performance.vo

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import java.time.LocalDate
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class PerformancePeriod
private constructor(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    init {
        if (endDate.isBefore(startDate)) {
            throw DomainException(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD)
        }
    }

    companion object {
        fun of(startDate: LocalDate, endDate: LocalDate): PerformancePeriod =
            PerformancePeriod(startDate, endDate)

        fun fromPerformanceDateTimes(performanceDates: List<LocalDateTime>): PerformancePeriod =
            fromDates(performanceDates.map(LocalDateTime::toLocalDate))

        fun fromDates(performanceDates: List<LocalDate>): PerformancePeriod {
            validateNotEmpty(performanceDates)
            return PerformancePeriod(performanceDates.min(), performanceDates.max())
        }

        private fun validateNotEmpty(performanceDates: List<*>) {
            if (performanceDates.isEmpty()) {
                throw DomainException(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD)
            }
        }
    }
}
