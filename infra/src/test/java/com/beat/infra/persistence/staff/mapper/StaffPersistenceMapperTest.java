package com.beat.infra.persistence.staff.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.beat.domain.staff.domain.Staff;
import com.beat.infra.persistence.staff.entity.StaffJpaEntity;

class StaffPersistenceMapperTest {

	private final StaffPersistenceMapper mapper = new StaffPersistenceMapper();

	@Test
	void toDomainPreservesJpaEntityFieldsIncludingScalarPerformanceId() {
		StaffJpaEntity entity = StaffJpaEntity.rehydrate(
			11L,
			"staff-name",
			"staff-role",
			"https://example.com/staff.png",
			22L
		);

		Staff staff = mapper.toDomain(entity);

		assertAll(
			() -> assertEquals(11L, staff.getId()),
			() -> assertEquals("staff-name", staff.getStaffName()),
			() -> assertEquals("staff-role", staff.getStaffRole()),
			() -> assertEquals("https://example.com/staff.png", staff.getStaffPhoto()),
			() -> assertEquals(22L, staff.getPerformanceId())
		);
	}

	@Test
	void toEntityKeepsGeneratedIdNullForNewStaff() {
		Staff staff = Staff.create(
			"new-staff",
			"new-role",
			"https://example.com/new-staff.png",
			44L
		);

		StaffJpaEntity entity = mapper.toEntity(staff);

		assertAll(
			() -> assertNull(staff.getId()),
			() -> assertNull(entity.getId()),
			() -> assertEquals("new-staff", entity.getStaffName()),
			() -> assertEquals("new-role", entity.getStaffRole()),
			() -> assertEquals("https://example.com/new-staff.png", entity.getStaffPhoto()),
			() -> assertEquals(44L, entity.getPerformanceId())
		);
	}

	@Test
	void toEntityPreservesRehydratedDomainFields() {
		Staff staff = Staff.rehydrate(
			31L,
			"existing-staff",
			"existing-role",
			"https://example.com/existing-staff.png",
			41L
		);

		StaffJpaEntity entity = mapper.toEntity(staff);

		assertAll(
			() -> assertEquals(31L, entity.getId()),
			() -> assertEquals("existing-staff", entity.getStaffName()),
			() -> assertEquals("existing-role", entity.getStaffRole()),
			() -> assertEquals("https://example.com/existing-staff.png", entity.getStaffPhoto()),
			() -> assertEquals(41L, entity.getPerformanceId())
		);
	}
}
