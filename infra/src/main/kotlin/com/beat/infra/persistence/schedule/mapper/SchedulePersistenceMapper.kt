package com.beat.infra.persistence.schedule.mapper

import com.beat.domain.exception.DomainException
import com.beat.domain.schedule.model.Schedule
import com.beat.infra.persistence.exception.PersistenceMappingException
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import org.springframework.stereotype.Component

@Component
class SchedulePersistenceMapper {

    fun toDomain(entity: ScheduleJpaEntity): Schedule =
        try {
            Schedule.rehydrate(
                entity.id,
                entity.performanceDate,
                entity.bookingCloseAt,
                entity.totalTicketCount,
                entity.soldTicketCount,
                entity.scheduleNumber,
                entity.performanceId,
            )
        } catch (exception: DomainException) {
            throw PersistenceMappingException.invalidStoredState("Schedule", entity.id, exception)
        }

    fun toEntity(schedule: Schedule): ScheduleJpaEntity =
        ScheduleJpaEntity.rehydrate(
            schedule.getId(),
            schedule.getPerformanceDate(),
            schedule.getBookingCloseAt(),
            schedule.getTotalTicketCount(),
            schedule.getAllocatedTicketCount(),
            schedule.getScheduleNumber(),
            schedule.getPerformanceId(),
        )
}
