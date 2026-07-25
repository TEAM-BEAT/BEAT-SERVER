package com.beat.infra.persistence.schedule.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.exception.DomainException;
import com.beat.domain.schedule.model.Schedule;
import com.beat.infra.persistence.exception.PersistenceMappingException;
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity;

@Component
public class SchedulePersistenceMapper {

	public Schedule toDomain(ScheduleJpaEntity entity) {
		try {
			return Schedule.rehydrate(
				entity.getId(),
				entity.getPerformanceDate(),
				entity.getBookingCloseAt(),
				entity.getTotalTicketCount(),
				entity.getSoldTicketCount(),
				entity.getScheduleNumber(),
				entity.getPerformanceId()
			);
		} catch (DomainException exception) {
			throw PersistenceMappingException.invalidStoredState("Schedule", entity.getId(), exception);
		}
	}

	public ScheduleJpaEntity toEntity(Schedule schedule) {
		return ScheduleJpaEntity.rehydrate(
			schedule.getId(),
			schedule.getPerformanceDate(),
			schedule.getBookingCloseAt(),
			schedule.getTotalTicketCount(),
			schedule.getAllocatedTicketCount(),
			schedule.getScheduleNumber(),
			schedule.getPerformanceId()
		);
	}
}
