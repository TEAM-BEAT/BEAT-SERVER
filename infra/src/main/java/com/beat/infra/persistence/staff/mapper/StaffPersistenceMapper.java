package com.beat.infra.persistence.staff.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.performance.model.Staff;
import com.beat.infra.persistence.staff.entity.StaffJpaEntity;

@Component
public class StaffPersistenceMapper {

	public Staff toDomain(StaffJpaEntity entity) {
		return Staff.rehydrate(
			entity.getId(),
			entity.getStaffName(),
			entity.getStaffRole(),
			entity.getStaffPhoto()
		);
	}

	public StaffJpaEntity toEntity(Staff staff, long performanceId) {
		return StaffJpaEntity.rehydrate(
			staff.getId(),
			staff.getStaffName(),
			staff.getStaffRole(),
			staff.getStaffPhoto(),
			performanceId
		);
	}
}
