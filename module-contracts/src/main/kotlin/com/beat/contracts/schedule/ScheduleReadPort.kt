package com.beat.contracts.schedule

import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel
import com.beat.contracts.schedule.readmodel.ScheduleSummaryReadModel

interface ScheduleReadPort {

    fun findMinPerformanceDateByPerformanceIds(performanceIds: List<Long>): List<MinPerformanceDateReadModel>

    fun findAllByPerformanceId(performanceId: Long): List<ScheduleSummaryReadModel>
}
