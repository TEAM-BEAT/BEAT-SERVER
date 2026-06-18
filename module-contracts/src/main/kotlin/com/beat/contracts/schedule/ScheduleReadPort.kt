package com.beat.contracts.schedule

import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel

fun interface ScheduleReadPort {

    fun findMinPerformanceDateByPerformanceIds(performanceIds: List<Long>): List<MinPerformanceDateReadModel>
}
