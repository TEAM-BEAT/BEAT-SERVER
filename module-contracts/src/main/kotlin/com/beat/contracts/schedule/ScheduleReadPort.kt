package com.beat.contracts.schedule

import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel

interface ScheduleReadPort {

    fun findMinPerformanceDateByPerformanceIds(performanceIds: List<Long>): List<MinPerformanceDateReadModel>
}
