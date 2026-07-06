package com.beat.contracts.schedule

import com.beat.contracts.schedule.readmodel.PerformanceScheduleAvailabilityReadModel

fun interface PerformanceScheduleReadPort {

    fun findAllByPerformanceId(performanceId: Long): List<PerformanceScheduleAvailabilityReadModel>
}
