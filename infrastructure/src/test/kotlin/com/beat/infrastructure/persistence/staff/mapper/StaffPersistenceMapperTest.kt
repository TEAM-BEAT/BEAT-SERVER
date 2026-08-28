package com.beat.infrastructure.persistence.staff.mapper

import com.beat.domain.performance.model.Staff
import com.beat.infrastructure.persistence.staff.entity.StaffJpaEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StaffPersistenceMapperTest :
    FunSpec({
        val mapper = StaffPersistenceMapper()

        test("toDomain은 scalar performanceId를 포함해 JPA entity 필드를 보존한다") {
            val entity =
                StaffJpaEntity.rehydrate(
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

        test("toEntity는 신규 staff의 생성 id를 null로 유지한다") {
            val staff =
                Staff.create(
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

        test("toEntity는 재구성된 domain 필드를 보존한다") {
            val staff =
                Staff.rehydrate(
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
