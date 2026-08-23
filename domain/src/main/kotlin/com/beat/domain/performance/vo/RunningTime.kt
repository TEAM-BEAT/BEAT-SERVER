package com.beat.domain.performance.vo

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class RunningTime private constructor(
    val minutes: Int,
) {
    fun endsAt(start: LocalDateTime): LocalDateTime = start.plusMinutes(minutes.toLong())

    companion object {
        fun of(minutes: Int): RunningTime {
            if (minutes <= 0) {
                throw DomainException(PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME)
            }
            return RunningTime(minutes)
        }
    }
}
