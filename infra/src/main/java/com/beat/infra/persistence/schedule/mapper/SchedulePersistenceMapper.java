package com.beat.infra.persistence.schedule.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.schedule.domain.Schedule;
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity;

@Component
public class SchedulePersistenceMapper {

	public Schedule toDomain(ScheduleJpaEntity entity) {
			return Schedule.rehydrate(
				entity.getId(),
				entity.getPerformanceDate(),
				entity.getBookingCloseAt(),
				entity.getTotalTicketCount(),
				entity.getSoldTicketCount(),
			entity.getScheduleNumber(),
			entity.getPerformanceId()
		);
	}

	public ScheduleJpaEntity toEntity(Schedule schedule) {
			return ScheduleJpaEntity.rehydrate(
				schedule.getId(),
				schedule.getPerformanceDate(),
				schedule.getBookingCloseAt(),
				schedule.getTotalTicketCount(),
				schedule.getSoldTicketCount(),
			schedule.getScheduleNumber(),
			schedule.getPerformanceId()
		);
	}
}
