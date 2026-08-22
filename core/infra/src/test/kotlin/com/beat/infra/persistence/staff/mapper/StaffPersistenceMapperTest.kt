package com.beat.infra.persistence.staff.mapper

import com.beat.domain.performance.model.Staff
import com.beat.infra.persistence.staff.entity.StaffJpaEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StaffPersistenceMapperTest : FunSpec({
    val mapper = StaffPersistenceMapper()

    test("toDomainPreservesJpaEntityFieldsIncludingScalarPerformanceId") {
        val entity = StaffJpaEntity.rehydrate(
            11L,
            "staff-name",
            "staff-role",
            "https://example.com/staff.png",
            22L,
        )

        val staff = mapper.toDomain(entity)

        staff.id shouldBe 11L
        staff.staffName shouldBe "staff-name"
        staff.staffRole shouldBe "staff-role"
        staff.staffPhoto shouldBe "https://example.com/staff.png"
    }

    test("toEntityKeepsGeneratedIdNullForNewStaff") {
        val staff = Staff.create(
            "new-staff",
            "new-role",
            "https://example.com/new-staff.png",
        )

        val entity = mapper.toEntity(staff, 44L)

        staff.id shouldBe null
        entity.id shouldBe null
        entity.staffName shouldBe "new-staff"
        entity.staffRole shouldBe "new-role"
        entity.staffPhoto shouldBe "https://example.com/new-staff.png"
        entity.performanceId shouldBe 44L
    }

    test("toEntityPreservesRehydratedDomainFields") {
        val staff = Staff.rehydrate(
            31L,
            "existing-staff",
            "existing-role",
            "https://example.com/existing-staff.png",
        )

        val entity = mapper.toEntity(staff, 41L)

        entity.id shouldBe 31L
        entity.staffName shouldBe "existing-staff"
        entity.staffRole shouldBe "existing-role"
        entity.staffPhoto shouldBe "https://example.com/existing-staff.png"
        entity.performanceId shouldBe 41L
    }
})
