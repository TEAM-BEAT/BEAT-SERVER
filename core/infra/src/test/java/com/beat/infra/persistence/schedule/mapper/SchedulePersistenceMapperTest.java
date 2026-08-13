package com.beat.infra.persistence.schedule.mapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.infra.persistence.exception.PersistenceMappingException;
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity;

class SchedulePersistenceMapperTest {

	private final SchedulePersistenceMapper mapper = new SchedulePersistenceMapper();

	@Test
	void invalidStoredStateIsNotExposedAsAClientDomainError() {
		LocalDateTime performanceDate = LocalDateTime.of(2026, 7, 17, 20, 0);
		ScheduleJpaEntity corruptedRow = ScheduleJpaEntity.rehydrate(
			1L,
			performanceDate,
			performanceDate.minusMinutes(1),
			10,
			0,
			ScheduleNumber.FIRST,
			2L
		);

		assertThrows(PersistenceMappingException.class, () -> mapper.toDomain(corruptedRow));
	}
}
