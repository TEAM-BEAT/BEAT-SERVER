package com.beat.domain.schedule.service

import com.beat.domain.exception.DomainException
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.exception.ScheduleErrorCode

class ScheduleSequenceDomainService {
    fun validateScheduleCount(scheduleCount: Long) {
        if (scheduleCount > ScheduleNumber.entries.size) {
            throw DomainException(ScheduleErrorCode.TOO_MANY_SCHEDULES)
        }
    }

    fun assignScheduleNumbers(schedules: List<Schedule>): List<Schedule> {
        validateScheduleCount(schedules.size.toLong())
        validateSamePerformance(schedules)

        return schedules
            .sortedBy { it.performanceDate }
            .mapIndexed { index, schedule ->
                schedule.updateScheduleNumber(ScheduleNumber.entries[index])
            }
    }

    private fun validateSamePerformance(schedules: List<Schedule>) {
        val performanceIds = schedules.map(Schedule::performanceId).distinct()
        if (performanceIds.size > 1) {
            throw DomainException(ScheduleErrorCode.MIXED_PERFORMANCE_SCHEDULES)
        }
    }
}
