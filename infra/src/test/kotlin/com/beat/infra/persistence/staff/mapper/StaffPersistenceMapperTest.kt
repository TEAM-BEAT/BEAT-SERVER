package com.beat.infra.persistence.staff.mapper

import com.beat.domain.performance.model.Staff
import com.beat.infra.persistence.staff.entity.StaffJpaEntity
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StaffPersistenceMapperTest {
    private val mapper = StaffPersistenceMapper()

    @Test
    fun toDomainPreservesJpaEntityFieldsIncludingScalarPerformanceId() {
        val entity = StaffJpaEntity.rehydrate(
            11L,
            "staff-name",
            "staff-role",
            "https://example.com/staff.png",
            22L,
        )

        val staff = mapper.toDomain(entity)

        assertAll(
            { assertEquals(11L, staff.getId()) },
            { assertEquals("staff-name", staff.staffName) },
            { assertEquals("staff-role", staff.staffRole) },
            { assertEquals("https://example.com/staff.png", staff.staffPhoto) },
        )
    }

    @Test
    fun toEntityKeepsGeneratedIdNullForNewStaff() {
        val staff = Staff.create(
            "new-staff",
            "new-role",
            "https://example.com/new-staff.png",
        )

        val entity = mapper.toEntity(staff, 44L)

        assertAll(
            { assertNull(staff.getId()) },
            { assertNull(entity.id) },
            { assertEquals("new-staff", entity.staffName) },
            { assertEquals("new-role", entity.staffRole) },
            { assertEquals("https://example.com/new-staff.png", entity.staffPhoto) },
            { assertEquals(44L, entity.performanceId) },
        )
    }

    @Test
    fun toEntityPreservesRehydratedDomainFields() {
        val staff = Staff.rehydrate(
            31L,
            "existing-staff",
            "existing-role",
            "https://example.com/existing-staff.png",
        )

        val entity = mapper.toEntity(staff, 41L)

        assertAll(
            { assertEquals(31L, entity.id) },
            { assertEquals("existing-staff", entity.staffName) },
            { assertEquals("existing-role", entity.staffRole) },
            { assertEquals("https://example.com/existing-staff.png", entity.staffPhoto) },
            { assertEquals(41L, entity.performanceId) },
        )
    }
}
