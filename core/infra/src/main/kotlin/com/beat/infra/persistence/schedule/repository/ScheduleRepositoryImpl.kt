package com.beat.infra.persistence.schedule.repository

import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.infra.persistence.schedule.mapper.SchedulePersistenceMapper
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class ScheduleRepositoryImpl(
    private val scheduleJpaRepository: ScheduleJpaRepository,
    private val schedulePersistenceMapper: SchedulePersistenceMapper,
) : ScheduleRepository {

    override fun findById(id: Long?): Optional<Schedule> =
        scheduleJpaRepository.findById(requireRepositoryId(id)).map(schedulePersistenceMapper::toDomain)

    override fun findPerformanceIdById(id: Long): Long? =
        scheduleJpaRepository.findPerformanceIdById(id)

    override fun lockById(id: Long?): Optional<Schedule> =
        scheduleJpaRepository.lockById(id).map(schedulePersistenceMapper::toDomain)

    override fun isBeforeBookingCloseAt(id: Long?): Boolean =
        scheduleJpaRepository.isBeforeBookingCloseAt(id) == 1L

    override fun findAllByPerformanceId(performanceId: Long?): List<Schedule> =
        scheduleJpaRepository.findAllByPerformanceId(performanceId).map(schedulePersistenceMapper::toDomain)

    override fun findAllById(ids: Collection<Long>): List<Schedule> =
        scheduleJpaRepository.findAllById(ids).map(schedulePersistenceMapper::toDomain)

    override fun findIdsByPerformanceId(performanceId: Long?): List<Long> =
        scheduleJpaRepository.findIdsByPerformanceId(performanceId)

    override fun countByPerformanceId(performanceId: Long?): Int =
        scheduleJpaRepository.countByPerformanceId(performanceId)

    override fun save(schedule: Schedule): Schedule =
        scheduleJpaRepository.save(schedulePersistenceMapper.toEntity(schedule))
            .let(schedulePersistenceMapper::toDomain)

    override fun saveAll(schedules: List<Schedule>): List<Schedule> =
        scheduleJpaRepository.saveAll(schedules.map(schedulePersistenceMapper::toEntity))
            .map(schedulePersistenceMapper::toDomain)

    override fun delete(schedule: Schedule) {
        val scheduleId = requireNotNull(schedule.getId()) { "Cannot delete unpersisted Schedule" }
        scheduleJpaRepository.deleteById(scheduleId)
    }

    override fun deleteByPerformanceId(performanceId: Long?) {
        scheduleJpaRepository.deleteByPerformanceId(performanceId)
    }

    private fun requireRepositoryId(id: Long?): Long =
        requireNotNull(id) { "The given id must not be null" }
}
