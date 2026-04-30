package com.beat.domain.schedule.repository

import com.beat.domain.schedule.domain.Schedule
import java.util.*

@JvmSuppressWildcards
interface ScheduleRepository {
    fun findById(id: Long?): Optional<Schedule>

    fun lockById(id: Long?): Optional<Schedule>

    fun findAllByPerformanceId(performanceId: Long?): List<Schedule>

    fun findAllById(ids: Collection<Long>): List<Schedule>

    fun findIdsByPerformanceId(performanceId: Long?): List<Long>

    fun countByPerformanceId(performanceId: Long?): Int

    fun save(schedule: Schedule): Schedule

    fun saveAll(schedules: List<Schedule>): List<Schedule>

    fun delete(schedule: Schedule)

    fun deleteByPerformanceId(performanceId: Long?)

    fun findPendingSchedules(): List<Schedule>
}
