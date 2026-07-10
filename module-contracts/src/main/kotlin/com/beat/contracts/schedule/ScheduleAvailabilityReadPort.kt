package com.beat.contracts.schedule

import com.beat.contracts.schedule.readmodel.ScheduleAvailabilityReadModel

fun interface ScheduleAvailabilityReadPort {

    fun findAllByPerformanceId(performanceId: Long): List<ScheduleAvailabilityReadModel>
}
